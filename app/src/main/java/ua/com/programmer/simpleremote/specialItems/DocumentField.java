package ua.com.programmer.simpleremote.specialItems;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import ua.com.programmer.simpleremote.Utils;

public class DocumentField {

    private String meta;
    public String type;
    public String name;
    public String description;
    public String code;
    public String value;

    private final Utils utils = new Utils();

    public DocumentField(String init){
        setDefaults();

        if (init.equals("")) return;

        try {
            JSONObject jsonObject = new JSONObject(init);
            initializeFromJSON(jsonObject);
        }catch (JSONException ex){
            utils.log("e", "DocumentField init from string: "+ex.toString());
        }
    }

    public DocumentField(JSONObject jsonObject){
        setDefaults();
        initializeFromJSON(jsonObject);
    }

    private void setDefaults(){
        meta = "unknown";
        type = "";
        name = "";
        description = "";
        code = "";
        value = "";
    }

    private void initializeFromJSON(JSONObject jsonObject){
        JSONArray columnNames = jsonObject.names();
        for (int i = 0; i < Objects.requireNonNull(columnNames).length(); i++) {
            try {
                String columnName = columnNames.getString(i);
                String elementValue = jsonObject.getString(columnName);

                if (columnName.equals("meta")) meta = elementValue;
                if (columnName.equals("name")) name = elementValue;
                if (columnName.equals("type")) type = elementValue;
                if (columnName.equals("description")) description = elementValue;
                if (columnName.equals("code")) code = elementValue;
                if (columnName.equals("value")) value = elementValue;

            }catch (JSONException ex){
                utils.log("e", "DocumentField init from JSON: "+ex.toString());
            }
        }
    }

    public String getNamedValue(){
        return description+": "+value;
    }

    public boolean hasValue(){
        return !value.equals("");
    }

    public boolean isReal() { return !name.equals(""); }

    public String asString(){
        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put("meta",meta);
            jsonObject.put("type",type);
            jsonObject.put("name",name);
            jsonObject.put("description",description);
            jsonObject.put("code",code);
            jsonObject.put("value",value);
        }catch (JSONException ex){
            utils.log("e","DocumentField:asString: "+ex.toString());
        }
        return jsonObject.toString();
    }

    public boolean isCatalog(){
        return meta.equals("reference");
    }

    public boolean isDate(){
        return meta.equals("date");
    }
}
