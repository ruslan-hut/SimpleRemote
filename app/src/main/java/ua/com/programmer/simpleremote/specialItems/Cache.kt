package ua.com.programmer.simpleremote.specialItems

import java.util.HashMap
import java.util.UUID

class Cache private constructor() {
    fun setItemHolder(item: DataBaseItem?) {
        itemHolder = item
    }

    fun getItemHolder(): DataBaseItem {
        if (itemHolder == null) {
            itemHolder = DataBaseItem()
        }
        return itemHolder!!
    }

    fun put(dataBaseItem: DataBaseItem?): String {
        val key = UUID.randomUUID().toString()
        map.put(key, dataBaseItem)
        return key
    }

    fun get(key: String?): DataBaseItem {
        if (key != null && map.containsKey(key)) {
            val item: DataBaseItem? = map.get(key)
            if (item != null) {
                return item
            }
        }
        return DataBaseItem()
    }

    fun clear() {
        map.clear()
    }

    fun getFragmentTAG(): String {
        if (fragmentTAG == null) {
            fragmentTAG = ""
        }
        return fragmentTAG!!
    }

    fun setFragmentTAG(tag: String?) {
        fragmentTAG = tag
    }

    companion object {
        private var itemHolder: DataBaseItem? = null
        private val map = HashMap<String?, DataBaseItem?>()
        private var fragmentTAG: String? = null
        private var cache: Cache? = null

        fun getInstance(): Cache {
            if (cache == null) cache = Cache()
            return cache!!
        }
    }
}
