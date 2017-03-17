package com.mediadriver.atlas.validators.v2;

/**
 * Created by slepage on 3/15/17.
 */
public class PositiveIntegerValidator implements Validator {


    private String violationMessage;

    private String field;

    public PositiveIntegerValidator(String field, String violationMessage) {
        this.violationMessage = violationMessage;
        this.field = field;
    }

    @Override
    public boolean supports(Class clazz) {

        if (Integer.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz)) {
            return true;
        }

        return false;
    }

    @Override
    public void validate(Object target, Errors errors) {
        this.validate(target, errors, AtlasMappingError.Level.ERROR);
    }

    @Override
    public void validate(Object target, Errors errors, AtlasMappingError.Level level) {
        Integer value = (Integer) target;
        if (value == null || value < 0) {
            errors.addError(new AtlasMappingError(field, target, violationMessage, level));
        }
    }
}
