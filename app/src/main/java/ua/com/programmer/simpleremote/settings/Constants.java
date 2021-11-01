package ua.com.programmer.simpleremote.settings;

public final class Constants {

    public final static String DOCUMENTS = "documents";
    public final static String CATALOGS = "catalogs";
    public final static String DOCUMENTS_LIST = "documents_list";
    public final static String CACHED_DOCUMENTS = "cached_documents";
    public final static String ACTION_SAVE_DOCUMENT = "saveDocument";

    //document types
    public final static String RECEIVE = "receive";
    public final static String SHIP = "ship";
    public final static String INVENTORY = "inventory";

    //catalog types
    public final static String CONTRACTORS = "contractors";
    public final static String GOODS = "goods";
    public final static String STORES = "stores";

    //common fields
    public final static String TYPE = "type";
    public final static String GUID = "guid";

    //document fields names
    public final static String CACHE_GUID = "cacheGUID"; //field name for document data object
    public final static String DOCUMENT_NUMBER = "number";
    public final static String DOCUMENT_DATE = "date";
    public final static String DOCUMENT_IS_DELETED = "isDeleted";
    public final static String DOCUMENT_IS_PROCESSED = "isProcessed";

    //working modes
    public final static String MODE_FULL = "full";
    public final static String MODE_COLLECT = "collect";
}
