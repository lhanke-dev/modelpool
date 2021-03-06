package de.lhankedev.modelpool.assertion;

import de.lhankedev.modelpool.model.AttributeDefinition;
import de.lhankedev.modelpool.model.ObjectDefinition;

import java.util.Optional;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class ObjectDefinitionAssertion extends AbstractAssert<ObjectDefinitionAssertion, ObjectDefinition> {

    private ObjectDefinitionAssertion(final ObjectDefinition actual) {
        super(actual, ObjectDefinitionAssertion.class);
    }

    public static ObjectDefinitionAssertion assertThat(final ObjectDefinition actual) {
        return new ObjectDefinitionAssertion(actual);
    }

    public ObjectDefinitionAssertion hasId(final String id) {
        final Optional<String> actualId = actual.getId();
        Assertions.assertThat(actualId)
                .isPresent()
                .contains(id);
        return this;
    }

    public ObjectDefinitionAssertion hasParent(final String parent) {
        final Optional<String> actualParent = actual.getParent();
        Assertions.assertThat(actualParent)
                .isPresent()
                .contains(parent);
        return this;
    }

    public ObjectDefinitionAssertion hasType(final String type) {
        Assertions.assertThat(actual.getType())
                .isEqualTo(type);
        return this;
    }

    public ObjectDefinitionAssertion hasAttributeCount(final int attributeCount) {
        Assertions.assertThat(actual.getAttributes())
                .isNotNull()
                .hasSize(attributeCount);
        return this;
    }

    public AttributeDefinitionAssertion getAttribute(final int index) {
        Assertions.assertThat(actual.getAttributes())
                .isNotNull();
        Assertions.assertThat(actual.getAttributes().size())
                .isGreaterThan(index);
        return AttributeDefinitionAssertion
                .assertThat(actual.getAttributes().get(index))
                .isNotNull();
    }

    public AttributeDefinitionAssertion getAttributeByName(final String name) {
        final Optional<AttributeDefinition> targetAttribute = actual.getAttributes().stream()
                .filter(attribute -> name.equals(attribute.getAttributeName()))
                .findFirst();
        Assertions.assertThat(targetAttribute)
                .isPresent();
        return AttributeDefinitionAssertion
                .assertThat(targetAttribute.get());
    }
}
