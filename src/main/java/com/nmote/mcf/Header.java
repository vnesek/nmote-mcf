package com.nmote.mcf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.codec.EncoderUtil.Usage;

import java.io.Serializable;
import java.util.*;

public class Header implements Serializable {

    private static final long serialVersionUID = 5168943555857734019L;

    public Header() {
    }

    public Header(Header header) {
        if (header.fields != null) {
            Map<String, List<HeaderField>> f = new HashMap<>();
            for (Map.Entry<String, List<HeaderField>> e : header.fields.entrySet()) {
                f.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            this.fields = f;
        }
    }

    public void add(String name, String value) {
        String key = toKey(name);
        List<HeaderField> field = getFields().get(key);
        if (field == null) {
            field = new ArrayList<>();
            getFields().put(key, field);
        }
        field.add(new HeaderField(name, value));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Header)) {
            return false;
        }
        Header other = (Header) obj;
        if (fields == null) {
            if (other.fields != null) {
                return false;
            }
        } else if (!fields.equals(other.fields)) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public Set<HeaderField> fields() {
        Set<HeaderField> result = new HashSet<>();
        if (fields != null) {
            for (List<HeaderField> f : getFields().values()) {
                result.addAll(f);
            }
        }
        return result;
    }

    public String get(String name) {
        if (this.fields != null) {
            List<HeaderField> field = getFields().get(toKey(name));
            return field != null ? field.get(0).getValue() : null;
        } else {
            return null;
        }
    }

    public List<String> getAll(String name) {
        return getAll(name, false);
    }

    public List<String> getAll(String name, boolean exact) {
        List<String> result = null;
        if (this.fields != null) {
            List<HeaderField> fields = getFields().get(toKey(name));
            if (fields != null) {
                int len = fields.size();
                if (len > 0) {
                    result = new ArrayList<>(len);
                    for (HeaderField e : fields) {
                        if (!exact || name.equals(e.getName())) {
                            result.add(e.getValue());
                        }
                    }
                }
            }
        }
        return result;
    }

    @JsonIgnore
    public Set<String> getFieldNames() {
        return fields != null ? getFields().keySet() : Collections.<String>emptySet();
    }

    public Map<String, List<HeaderField>> getFields() {
        if (fields == null) {
            fields = new LinkedHashMap<>();
        }
        return fields;
    }

    @Override
    public int hashCode() {
        final int prime = 97;
        int result = 1;
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        return result;
    }

    public List<HeaderField> remove(String name) {
        return getFields().remove(toKey(name));
    }

    public void set(String name, String value) {
        Map<String, List<HeaderField>> fields = getFields();
        String key = toKey(name);
        if (value != null) {
            List<HeaderField> field = fields.get(key);
            if (field != null) {
                field.clear();
            } else {
                field = new ArrayList<>();
                fields.put(key, field);
            }
            field.add(new HeaderField(name, value));
        } else {
            getFields().remove(key);
        }
    }

    @Override
    public String toString() {
        StringBuilder w = new StringBuilder(4096);
        for (List<HeaderField> hfs : getFields().values()) {
            for (HeaderField hf : hfs) {
                w.append(hf.getName());
                w.append(": ");
                if (hf.getName().equalsIgnoreCase("subject")) {
                    w.append(EncoderUtil.encodeIfNecessary(hf.getValue(), Usage.TEXT_TOKEN, hf.getName().length() + 2));
                } else {
                    w.append(hf.getValue());
                }
                w.append('\n');
            }
        }
        w.append('\n');
        return w.toString();
    }

    private String toKey(String name) {
        String key = StringUtils.trimToNull(StringUtils.lowerCase(name));
        if (key == null) {
            throw new IllegalArgumentException("invalid key: '" + name + '\'');
        }
        return key;
    }

    private Map<String, List<HeaderField>> fields;

    public static class HeaderField implements Comparable<HeaderField>, Serializable {

        private static final long serialVersionUID = -140312000804965966L;

        public HeaderField() {
        }

        public HeaderField(HeaderField field) {
            this(field.getName(), field.getValue());
        }

        public HeaderField(String name, String value) {
            if (name == null) {
                throw new NullPointerException("field name is null");
            }
            if (value == null) {
                throw new NullPointerException("field value is null");
            }
            this.name = name;
            this.value = value;
        }

        @Override
        public int compareTo(HeaderField other) {
            int v = name.compareToIgnoreCase(other.name);
            if (v == 0) {
                v = value.compareTo(other.value);
            }
            return v;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof HeaderField)) {
                return false;
            }
            HeaderField other = (HeaderField) obj;
            return name.equalsIgnoreCase(other.name) && value.equals(other.value);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            final int prime = 19;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return name + ": " + value;
        }

        private String name;

        private String value;
    }
}
