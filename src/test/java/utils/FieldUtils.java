package utils;

import com.jacky8399.balancedvillagertrades.fields.ContainerField;
import com.jacky8399.balancedvillagertrades.fields.Field;

public class FieldUtils {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> void set(T root, ContainerField<T, ?> field, String fieldName, Object value) {
        ((Field) field.getFieldWrapped(fieldName)).set(root, value);
    }
}
