import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Nate on 5/23/16.
 */
public class Preset {
    private String desc, query;
    private ArrayList<Class> classes;

    public Preset(String desc, String query, String[] classes) {
        this.desc = desc;
        this.query = query;
        for(String clazz: classes) {
            try {
                this.classes.add(Class.forName(clazz));
            } catch(ClassNotFoundException e) {
                try {
                    this.classes.add(Class.forName("java.lang." + clazz));
                } catch(ClassNotFoundException e1) {
                    System.err.println("Class " + clazz + "could not be found");
                }
            }
        }
    }

    public String getDesc() {
        return desc;
    }

    public String getQuery() {
        return query;
    }

    public ArrayList<Class> getClasses() {
        return classes;
    }

    public static class PresetStringConverter extends StringConverter<Preset> {

        private ArrayList<Preset> presets;

        public PresetStringConverter(ArrayList<Preset> presets) {
            this.presets = presets;
        }

        @Override
        public String toString(Preset object) {
            return object.getDesc();
        }

        @Override
        public Preset fromString(String string) {
            for(Preset preset: presets) {
                if(preset.desc.equals(string))
                    return preset;
            }
            return null;
        }
    }
}
