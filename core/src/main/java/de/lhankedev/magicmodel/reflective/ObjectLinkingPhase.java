package de.lhankedev.magicmodel.reflective;

import de.lhankedev.magicmodel.model.AttributeDefinition;
import de.lhankedev.magicmodel.model.ObjectDefinition;
import lombok.Value;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

public class ObjectLinkingPhase implements ModelCreationPhase {

    @Value
    private class ObjectReference {

        List<Object> targetObjects = new ArrayList<>();
        String attributeName;
        Object owner;

        ObjectReference addTargetObjects(final Stream<Object> targetObjects) {
            targetObjects.forEach(this.targetObjects::add);
            return this;
        }

    }

    private final MagicModelReflections reflections;

    ObjectLinkingPhase() {
        this.reflections = new MagicModelReflections();
    }

    @Override
    public ModelCreationContext perform(ModelCreationContext context) {
        List<ObjectReference> objectReferences = new ArrayList<>();
        context.getParsedModel().getObjects().stream()
                .forEach(parsedObject -> this.addObjectReferences(parsedObject, objectReferences, context.getMapBasedMagicModel()));
        objectReferences.forEach(objectReference -> injectObjectReference(context, objectReference));
        return context;
    }

    private void injectObjectReference(ModelCreationContext context, ObjectReference objectReference) {
        try {
            final Field field = objectReference.getOwner().getClass().getDeclaredField(objectReference.getAttributeName());
            Object valueToSet = reflections.alignOrUnwrapCollectionType(field, objectReference.getTargetObjects());
            reflections.injectValueIntoField(field, objectReference.getOwner(), valueToSet);
        } catch (NoSuchFieldException e) {
            throw new StreamSupportingModelCreationException(
                    format("Could not inject field value for field %s with value %s into object of type %s",
                            objectReference.getAttributeName(), objectReference.getTargetObjects(), objectReference.getOwner().getClass()), e);
        }
    }

    private void addObjectReferences(ObjectDefinition objectDefinition, List<ObjectReference> objectReferences, MapBasedMagicModel magicModel) {
        Object modelObject = magicModel.getObjectByDefinition(objectDefinition)
                .orElseThrow(() -> new StreamSupportingModelCreationException(
                        format("Did not find created object for definition %s.",
                                objectDefinition))).getCreatedObject();

        objectDefinition.getAttributes().stream()
                .filter(this::containsObjectReferences)
                .map(attributeDefinition -> createForwardObjectReference(attributeDefinition, magicModel, modelObject))
                .forEach(forwardReference -> objectReferences.add(forwardReference));

        Optional<ObjectReference> parentReferenceOpt = objectDefinition.getParentObjectId()
                .map(parentObjectId -> this.createReferenceByParentId(parentObjectId, objectDefinition.getParentAttributeName(), modelObject, magicModel));
        mergeParentReferenceIntoExistingReferences(objectReferences, parentReferenceOpt);
    }

    private ObjectReference createForwardObjectReference(AttributeDefinition attributeDefinition, MapBasedMagicModel magicModel, Object modelObject) {
        return new ObjectReference(attributeDefinition.getAttributeName(), modelObject)
                .addTargetObjects(attributeDefinition.getAttributeValues().stream().map(this::stripObjectReferencePrefix)
                        .map(targetId -> magicModel.getObjectById(targetId, Object.class))
                        .filter(Optional::isPresent)
                        .map(Optional::get));
    }

    private void mergeParentReferenceIntoExistingReferences(List<ObjectReference> objectReferences, Optional<ObjectReference> parentReferenceOpt) {
        parentReferenceOpt.ifPresent(
                parentReference -> objectReferences.stream()
                        .filter(objectReference -> parentReference.getOwner() == objectReference.getOwner() && parentReference.getAttributeName().equals(objectReference.getAttributeName()))
                        .findFirst()
                        .ifPresentOrElse(
                                objectReference -> objectReference.getTargetObjects().addAll(parentReference.getTargetObjects()),
                                () -> objectReferences.add(parentReference)
                        )
        );
    }

    private ObjectReference createReferenceByParentId(String parentObjectId, Optional<String> parentAttributeNameOpt, Object modelObject, MapBasedMagicModel magicModel) {
        final Object owner = magicModel.getObjectById(parentObjectId, Object.class).orElseThrow(
                () -> new StreamSupportingModelCreationException(format("Did not find parent object with id %s", parentObjectId))
        );
        return parentAttributeNameOpt
                .map(parentAttributeName -> new ObjectReference(parentAttributeName, owner))
                .orElseGet(() -> new ObjectReference(reflections.findAttributeNameWithType(owner.getClass(), modelObject.getClass()).orElseThrow(() ->
                        new StreamSupportingModelCreationException(format("Did not find attribute with type %s in referenced parent class %s please use explicit references via id.",
                                modelObject.getClass(), owner.getClass()))),
                        owner))
                .addTargetObjects(Stream.of(modelObject));
    }


    private String stripObjectReferencePrefix(String targetIdValueWithPrefix) {
        return targetIdValueWithPrefix.substring(AttributeDefinition.OBJECT_REFERENCE_PREFIX.length());
    }

    private boolean containsObjectReferences(AttributeDefinition attributeDefinition) {
        return attributeDefinition.getAttributeValues().stream()
                .anyMatch(value -> value.startsWith(AttributeDefinition.OBJECT_REFERENCE_PREFIX));
    }
}
