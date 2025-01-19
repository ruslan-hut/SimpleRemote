package ua.com.programmer.simpleremote.deprecated.settings

object Constants {
    const val DOCUMENTS: String = "documents"
    const val CATALOGS: String = "catalogs"
    const val DOCUMENTS_LIST: String = "documents_list"
    const val CACHED_DOCUMENTS: String = "cached_documents"
    const val ACTION_SAVE_DOCUMENT: String = "saveDocument"
    const val MESSAGE: String = "message"

    //document types
    const val RECEIVE: String = "receive"
    const val SHIP: String = "ship"
    const val INVENTORY: String = "inventory"

    //catalog types
    const val CONTRACTORS: String = "contractors"
    const val GOODS: String = "goods"
    const val STORES: String = "stores"

    //common fields
    const val TYPE: String = "type"
    const val GUID: String = "guid"

    //document fields names
    const val CACHE_GUID: String = "cacheGUID" //field name for document data object
    const val DOCUMENT_NUMBER: String = "number"
    const val DOCUMENT_DATE: String = "date"
    const val DOCUMENT_IS_DELETED: String = "isDeleted"
    const val DOCUMENT_IS_PROCESSED: String = "isProcessed"
    const val DOCUMENT_LINE: String = "documentLine"

    //working modes
    const val MODE_FULL: String = "full"
    const val MODE_COLLECT: String = "collect"
}
