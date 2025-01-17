package ua.com.programmer.simpleremote.specialItems;

import java.util.HashMap;
import java.util.UUID;

public class Cache {

    private static DataBaseItem itemHolder;
    private static final HashMap<String, DataBaseItem> map = new HashMap<>();
    private static String fragmentTAG;
    private static Cache cache;

    private Cache(){}

    public static Cache getInstance(){
        if (cache == null) cache = new Cache();
        return cache;
    }

    public void setItemHolder(DataBaseItem item){
        itemHolder = item;
    }

    public DataBaseItem getItemHolder(){
        if (itemHolder == null){
            itemHolder = new DataBaseItem();
        }
        return itemHolder;
    }

    public String put(DataBaseItem dataBaseItem){
        String key = UUID.randomUUID().toString();
        map.put(key, dataBaseItem);
        return key;
    }

    public DataBaseItem get(String key){
        if (key != null && map.containsKey(key)){
            DataBaseItem item = map.get(key);
            if (item != null){
                return item;
            }
        }
        return new DataBaseItem();
    }

    public void clear(){
        map.clear();
    }

    public String getFragmentTAG(){
        if (fragmentTAG == null){
            fragmentTAG = "";
        }
        return fragmentTAG;
    }

    public void setFragmentTAG(String tag){
        fragmentTAG = tag;
    }
}
