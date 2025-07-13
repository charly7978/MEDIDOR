package com.example.myapplication.measurement.entity

import java.util.*

/**
 * Implementación simple de una caché LRU (Least Recently Used).
 * @param maxSize Tamaño máximo de la caché
 */
class LruCache<K, V>(private val maxSize: Int) {
    private val cache = LinkedHashMap<K, V>(maxSize, 0.75f, true)
    private val lock = java.util.concurrent.locks.ReentrantLock()
    
    /**
     * Obtiene un valor de la caché o lo calcula si no está presente.
     * @param key Clave del valor a obtener
     * @param compute Función para calcular el valor si no está en caché
     * @return El valor de la caché o el calculado
     */
    fun getOrCompute(key: K, compute: () -> V): V {
        return lock.withLock {
            cache[key] ?: compute().also { value ->
                if (cache.size >= maxSize) {
                    val eldest = cache.entries.iterator().next()
                    cache.remove(eldest.key)
                }
                cache[key] = value
            }
        }
    }
    
    /**
     * Obtiene un valor de la caché.
     * @return El valor o null si no existe
     */
    operator fun get(key: K): V? = lock.withLock { cache[key] }
    
    /**
     * Almacena un valor en la caché.
     */
    operator fun set(key: K, value: V) = lock.withLock {
        if (cache.size >= maxSize) {
            val eldest = cache.entries.iterator().next()
            cache.remove(eldest.key)
        }
        cache[key] = value
    }
    
    /**
     * Elimina un valor de la caché.
     */
    fun remove(key: K) = lock.withLock { cache.remove(key) }
    
    /**
     * Limpia toda la caché.
     */
    fun clear() = lock.withLock { cache.clear() }
    
    /**
     * Ejecuta un bloque de código con el bloqueo adquirido.
     */
    private fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }
}
