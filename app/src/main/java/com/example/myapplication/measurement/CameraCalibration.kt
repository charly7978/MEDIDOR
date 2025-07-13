package com.example.myapplication.measurement

import android.graphics.PointF
import kotlin.math.*

/**
 * Clase para manejar la calibración de la cámara y corregir distorsiones.
 * Implementa el modelo de cámara pinhole con distorsión radial y tangencial.
 */
class CameraCalibration(
    /** Ancho de la imagen en píxeles */
    private val imageWidth: Int,
    /** Alto de la imagen en píxeles */
    private val imageHeight: Int,
    /** Distancia focal en píxeles (fx, fy) */
    private val focalLength: PointF,
    /** Punto principal (cx, cy) en píxeles */
    private val principalPoint: PointF,
    /** Coeficientes de distorsión radial [k1, k2, p1, p2, k3, ...] */
    private val distortionCoeffs: FloatArray = FloatArray(5),
    /** Tamaño del sensor en mm */
    private val sensorSize: PointF = PointF(4.8f, 3.6f) // Tamaño típico para móviles
) {
    // Matriz de parámetros intrínsecos de la cámara (3x3)
    private val cameraMatrix = arrayOf(
        floatArrayOf(focalLength.x, 0f, principalPoint.x),
        floatArrayOf(0f, focalLength.y, principalPoint.y),
        floatArrayOf(0f, 0f, 1f)
    )
    
    // Factor de conversión píxeles a metros
    private val pixelToMeterX = sensorSize.x / imageWidth
    private val pixelToMeterY = sensorSize.y / imageHeight
    
    /**
     * Corrige la distorsión de la lente en un punto de la imagen.
     * 
     * @param point Punto en coordenadas de píxeles (distorsionado)
     * @return Punto corregido (sin distorsión) en coordenadas de píxeles
     */
    fun undistortPoint(point: PointF): PointF {
        // Convertir a coordenadas normalizadas
        val x = (point.x - principalPoint.x) / focalLength.x
        val y = (point.y - principalPoint.y) / focalLength.y
        
        val (undistX, undistY) = undistortNormalizedPoint(x, y)
        
        // Convertir de vuelta a coordenadas de píxeles
        return PointF(
            undistX * focalLength.x + principalPoint.x,
            undistY * focalLength.y + principalPoint.y
        )
    }
    
    /**
     * Corrige la distorsión en coordenadas normalizadas (sin unidades).
     * 
     * @param x Coordenada x normalizada (centrada en el punto principal)
     * @param y Coordenada y normalizada (centrada en el punto principal)
     * @return Par (x, y) corregido
     */
    private fun undistortNormalizedPoint(x: Float, y: Float): Pair<Float, Float> {
        // Para la corrección, usamos el método iterativo de OpenCV
        val maxIterations = 5
        val epsilon = 1e-5f
        
        var xd = x
        var yd = y
        
        // Aplicamos el método de aproximación de punto fijo
        repeat(maxIterations) {
            val r2 = xd * xd + yd * yd
            val r4 = r2 * r2
            val r6 = r4 * r2
            
            val k1 = distortionCoeffs.getOrElse(0) { 0f }
            val k2 = distortionCoeffs.getOrElse(1) { 0f }
            val k3 = distortionCoeffs.getOrElse(4) { 0f }
            val p1 = distortionCoeffs.getOrElse(2) { 0f }
            val p2 = distortionCoeffs.getOrElse(3) { 0f }
            
            val radial = 1f + k1 * r2 + k2 * r4 + k3 * r6
            val xpp = xd * radial + 2f * p1 * xd * yd + p2 * (r2 + 2f * xd * xd)
            val ypp = yd * radial + p1 * (r2 + 2f * yd * yd) + 2f * p2 * xd * yd
            
            val dx = x - xpp
            val dy = y - ypp
            
            xd += dx
            yd += dy
            
            if (dx * dx + dy * dy < epsilon) {
                return@repeat
            }
        }
        
        return xd to yd
    }
    
    /**
     * Proyecta un punto 3D en el espacio a coordenadas 2D de la imagen.
     * 
     * @param point3d Punto 3D en coordenadas de la cámara (metros)
     * @return Punto 2D en coordenadas de píxeles, o null si está detrás de la cámara
     */
    fun projectPoint(point3d: Point3D): PointF? {
        // Si el punto está detrás del plano de la imagen, no es visible
        if (point3d.z <= 0) return null
        
        // Proyección perspectiva
        val x = point3d.x / point3d.z
        val y = point3d.y / point3d.z
        
        // Aplicar distorsión
        val r2 = x * x + y * y
        val r4 = r2 * r2
        val r6 = r4 * r2
        
        val k1 = distortionCoeffs.getOrElse(0) { 0f }.toDouble()
        val k2 = distortionCoeffs.getOrElse(1) { 0f }.toDouble()
        val k3 = distortionCoeffs.getOrElse(4) { 0f }.toDouble()
        val p1 = distortionCoeffs.getOrElse(2) { 0f }.toDouble()
        val p2 = distortionCoeffs.getOrElse(3) { 0f }.toDouble()
        
        val radial = 1.0 + k1 * r2 + k2 * r4 + k3 * r6
        val xd = x * radial + 2.0 * p1 * x * y + p2 * (r2 + 2.0 * x * x)
        val yd = y * radial + p1 * (r2 + 2.0 * y * y) + 2.0 * p2 * x * y
        
        // Aplicar parámetros intrínsecos
        val u = (focalLength.x * xd + principalPoint.x).toFloat()
        val v = (focalLength.y * yd + principalPoint.y).toFloat()
        
        // Verificar si el punto está dentro de los límites de la imagen
        return if (u in 0f..imageWidth.toFloat() && v in 0f..imageHeight.toFloat()) {
            PointF(u, v)
        } else {
            null
        }
    }
    
    /**
     * Reconstruye un rayo 3D a partir de un punto 2D en la imagen.
     * 
     * @param point2d Punto 2D en coordenadas de píxeles
     * @param depth Distancia al plano de la imagen (opcional)
     * @return Punto 3D en coordenadas de la cámara (metros), o null si no se puede reconstruir
     */
    fun unprojectPoint(point2d: PointF, depth: Float = 1f): Point3D? {
        // Convertir a coordenadas normalizadas (sin distorsión)
        val (x, y) = undistortNormalizedPoint(
            (point2d.x - principalPoint.x) / focalLength.x,
            (point2d.y - principalPoint.y) / focalLength.y
        )
        
        // Si no se proporciona profundidad, asumimos que el punto está en el plano z=1
        // Esto da la dirección del rayo
        return Point3D(
            x = x * depth,
            y = y * depth,
            z = depth,
            confidence = 1f
        )
    }
    
    /**
     * Calcula la distancia real (en metros) entre dos puntos en la imagen,
     * asumiendo que están en el mismo plano a una distancia conocida.
     * 
     * @param p1 Primer punto en la imagen (píxeles)
     * @param p2 Segundo punto en la imagen (píxeles)
     * @param planeDistance Distancia al plano que contiene los puntos (metros)
     * @return Distancia real entre los puntos en metros
     */
    fun calculateRealDistance(p1: PointF, p2: PointF, planeDistance: Float): Float {
        val ray1 = unprojectPoint(p1, 1f) ?: return 0f
        val ray2 = unprojectPoint(p2, 1f) ?: return 0f
        
        // Escalar los rayos a la distancia del plano
        val scale = planeDistance / ray1.z
        val point1 = Point3D(
            ray1.x * scale,
            ray1.y * scale,
            ray1.z * scale,
            confidence = 1f
        )
        
        val point2 = Point3D(
            ray2.x * scale,
            ray2.y * scale,
            ray2.z * scale,
            confidence = 1f
        )
        
        return point1.distanceTo(point2)
    }
    
    /**
     * Estima la distancia a un objeto de tamaño conocido.
     * 
     * @param objectSize Tamaño real del objeto en metros
     * @param pixelSize Tamaño del objeto en la imagen en píxeles
     * @return Distancia estimada en metros
     */
    fun estimateDistance(objectSize: Float, pixelSize: Float): Float {
        // Convertir tamaño de píxeles a metros en el sensor
        val sensorSize = pixelSize * pixelToMeterX
        
        // Usar triángulos semejantes: objectSize / distance = sensorSize / focalLength
        return (objectSize * focalLength.x) / sensorSize
    }
    
    /**
     * Obtiene el ángulo de visión horizontal de la cámara en radianes.
     */
    fun getHorizontalFov(): Double {
        return 2.0 * atan2(imageWidth * pixelToMeterX / 2.0, focalLength.x.toDouble())
    }
    
    /**
     * Obtiene el ángulo de visión vertical de la cámara en radianes.
     */
    fun getVerticalFov(): Double {
        return 2.0 * atan2(imageHeight * pixelToMeterY / 2.0, focalLength.y.toDouble())
    }
    
    /**
     * Crea una instancia con parámetros predeterminados para una cámara típica de smartphone.
     * 
     * @param width Ancho de la imagen en píxeles
     * @param height Alto de la imagen en píxeles
     * @param focalLengthPx Distancia focal en píxeles (aproximada si no se proporciona)
     */
    companion object {
        fun createDefault(
            width: Int,
            height: Int,
            focalLengthPx: Float = -1f
        ): CameraCalibration {
            val focal = if (focalLengthPx > 0) {
                focalLengthPx
            } else {
                // Estimación basada en el campo de visión típico de un smartphone
                val diagonalPx = sqrt((width * width + height * height).toDouble())
                // Asumir un FoV diagonal de ~65 grados (1.134 radianes)
                (diagonalPx / (2.0 * tan(Math.toRadians(65.0) / 2.0))).toFloat()
            }
            
            return CameraCalibration(
                imageWidth = width,
                imageHeight = height,
                focalLength = PointF(focal, focal), // Asumir relación de aspecto 1:1
                principalPoint = PointF(width / 2f, height / 2f),
                // Coeficientes de distorsión típicos para una cámara de smartphone
                distortionCoeffs = floatArrayOf(
                    -0.2f,   // k1
                    0.1f,    // k2
                    0.0001f, // p1
                    0.0001f, // p2
                    0.05f    // k3
                )
            )
        }
    }
}

/**
 * Clase de ayuda para representar un punto 3D con confianza.
 */
data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val confidence: Float = 1f
) {
    /**
     * Calcula la distancia euclidiana a otro punto 3D.
     */
    fun distanceTo(other: Point3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Multiplica el punto por un escalar.
     */
    operator fun times(scalar: Float): Point3D {
        return Point3D(
            x * scalar,
            y * scalar,
            z * scalar,
            confidence
        )
    }
    
    /**
     * Suma otro punto a este punto.
     */
    operator fun plus(other: Point3D): Point3D {
        return Point3D(
            x + other.x,
            y + other.y,
            z + other.z,
            (confidence + other.confidence) / 2f
        )
    }
    
    /**
     * Resta otro punto a este punto.
     */
    operator fun minus(other: Point3D): Point3D {
        return Point3D(
            x - other.x,
            y - other.y,
            z - other.z,
            (confidence + other.confidence) / 2f
        )
    }
    
    /**
     * Producto punto con otro vector.
     */
    fun dot(other: Point3D): Float {
        return x * other.x + y * other.y + z * other.z
    }
    
    /**
     * Producto cruz con otro vector.
     */
    fun cross(other: Point3D): Point3D {
        return Point3D(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x,
            (confidence + other.confidence) / 2f
        )
    }
    
    /**
     * Normaliza el vector a longitud 1.
     */
    fun normalize(): Point3D {
        val length = sqrt(x * x + y * y + z * z)
        return if (length > 0) {
            Point3D(
                x / length,
                y / length,
                z / length,
                confidence
            )
        } else {
            this
        }
    }
    
    /**
     * Longitud (magnitud) del vector.
     */
    fun length(): Float {
        return sqrt(x * x + y * y + z * z)
    }
    
    /**
     * Ángulo entre este vector y otro, en radianes.
     */
    fun angleTo(other: Point3D): Float {
        val dot = dot(other)
        val len1 = length()
        val len2 = other.length()
        
        // Evitar división por cero
        if (len1 < 1e-6f || len2 < 1e-6f) {
            return 0f
        }
        
        // Asegurar que el valor esté en el rango [-1, 1] para acos
        val cosAngle = (dot / (len1 * len2)).coerceIn(-1f, 1f)
        return acos(cosAngle)
    }
}
