package com.example.myapplication.measurement

import kotlin.math.pow

/**
 * Implementación de un filtro de Kalman para fusión de sensores.
 * Este filtro se utiliza para combinar datos de múltiples sensores de manera óptima,
 * reduciendo el ruido y mejorando la precisión de las mediciones.
 *
 * @param stateDimension Dimensión del vector de estado (n)
 * @param measurementDimension Dimensión del vector de medición (m)
 * @param controlDimension Dimensión del vector de control (opcional, 0 si no se usa)
 */
class KalmanFilter(
    private val stateDimension: Int,
    private val measurementDimension: Int,
    private val controlDimension: Int = 0
) {
    // Matrices del filtro de Kalman
    private var state: Matrix                     // Vector de estado (n x 1)
    private var covariance: Matrix                // Matriz de covarianza del error (n x n)
    private var stateTransition: Matrix           // Matriz de transición de estado (n x n)
    private var controlMatrix: Matrix? = null     // Matriz de control (n x c), opcional
    private var measurementMatrix: Matrix         // Matriz de medición (m x n)
    private var processNoiseCov: Matrix           // Covarianza del ruido del proceso (n x n)
    private var measurementNoiseCov: Matrix       // Covarianza del ruido de medición (m x m)
    private var identity: Matrix                  // Matriz identidad (n x n)
    
    // Matrices temporales para cálculos intermedios
    private val tempMatrixNxN: Matrix
    private val tempMatrixNxM: Matrix
    private val tempMatrixMxN: Matrix
    private val tempMatrixMxM: Matrix
    private val tempVectorN: Matrix
    private val tempVectorM: Matrix
    
    // Tiempo de la última actualización (nanosegundos)
    private var lastUpdateTime: Long = 0L
    
    init {
        // Validar dimensiones
        require(stateDimension > 0) { "La dimensión del estado debe ser mayor que cero" }
        require(measurementDimension > 0) { "La dimensión de medición debe ser mayor que cero" }
        require(controlDimension >= 0) { "La dimensión de control no puede ser negativa" }
        
        // Inicializar matrices
        state = Matrix.zero(stateDimension, 1)
        covariance = Matrix.identity(stateDimension)
        stateTransition = Matrix.identity(stateDimension)
        measurementMatrix = Matrix.zero(measurementDimension, stateDimension)
        processNoiseCov = Matrix.identity(stateDimension) * 1e-4
        measurementNoiseCov = Matrix.identity(measurementDimension) * 1e-2
        identity = Matrix.identity(stateDimension)
        
        // Inicializar matrices temporales
        tempMatrixNxN = Matrix.zero(stateDimension, stateDimension)
        tempMatrixNxM = Matrix.zero(stateDimension, measurementDimension)
        tempMatrixMxN = Matrix.zero(measurementDimension, stateDimension)
        tempMatrixMxM = Matrix.zero(measurementDimension, measurementDimension)
        tempVectorN = Matrix.zero(stateDimension, 1)
        tempVectorM = Matrix.zero(measurementDimension, 1)
        
        // Inicializar matriz de control si es necesario
        if (controlDimension > 0) {
            controlMatrix = Matrix.zero(stateDimension, controlDimension)
        }
        
        // Establecer tiempo inicial
        lastUpdateTime = System.nanoTime()
    }
    
    /**
     * Realiza la predicción del siguiente estado.
     * 
     * @param controlVector Vector de control (opcional, null si no hay control)
     * @param deltaTime Tiempo transcurrido desde la última actualización en segundos
     */
    fun predict(controlVector: Matrix? = null, deltaTime: Double = 0.0) {
        // Actualizar tiempo
        val currentTime = System.nanoTime()
        val dt = if (deltaTime > 0) deltaTime 
                else (currentTime - lastUpdateTime).toDouble() / 1_000_000_000.0
        lastUpdateTime = currentTime
        
        // Actualizar matriz de transición de estado si se proporciona deltaTime
        if (dt > 0) {
            updateStateTransition(dt)
        }
        
        // Predicción del estado: x = F * x + B * u
        state = stateTransition * state
        
        // Aplicar control si está disponible
        controlVector?.let { u ->
            controlMatrix?.let { B ->
                state += B * u
            }
        }
        
        // Predicción de la covarianza: P = F * P * F^T + Q
        tempMatrixNxN.setToProduct(stateTransition, covariance)  // temp = F * P
        covariance.setToProduct(tempMatrixNxN, stateTransition.transposed())  // P = temp * F^T
        covariance += processNoiseCov  // P += Q
    }
    
    /**
     * Actualiza el filtro con una nueva medición.
     * 
     * @param measurement Vector de medición (m x 1)
     * @param measurementNoise Covarianza del ruido de medición (opcional)
     */
    fun update(measurement: Matrix, measurementNoise: Matrix? = null) {
        // Validar dimensiones
        require(measurement.rows == measurementDimension && measurement.cols == 1) {
            "La dimensión de la medición debe ser $measurementDimension x 1"
        }
        
        // Usar ruido de medición proporcionado o el predeterminado
        val R = measurementNoise ?: measurementNoiseCov
        
        // Residuo: y = z - H * x
        tempVectorM.setToProduct(measurementMatrix, state)  // H * x
        val y = measurement - tempVectorM                   // z - H * x
        
        // Matriz de innovación: S = H * P * H^T + R
        tempMatrixMxN.setToProduct(measurementMatrix, covariance)  // H * P
        tempMatrixMxM.setToProduct(tempMatrixMxN, measurementMatrix.transposed())  // H * P * H^T
        val S = tempMatrixMxM + R  // S = H * P * H^T + R
        
        // Ganancia de Kalman: K = P * H^T * S^-1
        tempMatrixNxM.setToProduct(covariance, measurementMatrix.transposed())  // P * H^T
        val K = tempMatrixNxM * S.inverse()  // K = P * H^T * S^-1
        
        // Actualizar estado: x = x + K * y
        tempVectorN.setToProduct(K, y)  // K * y
        state += tempVectorN  // x += K * y
        
        // Actualizar covarianza: P = (I - K * H) * P
        tempMatrixNxN.setToProduct(K, measurementMatrix)  // K * H
        tempMatrixNxN = identity - tempMatrixNxN  // I - K * H
        covariance = tempMatrixNxN * covariance  // P = (I - K * H) * P
    }
    
    /**
     * Actualiza la matriz de transición de estado basada en el tiempo transcurrido.
     * Este método debe ser sobrescrito por las subclases para modelos dinámicos.
     * 
     * @param dt Tiempo transcurrido en segundos
     */
    protected open fun updateStateTransition(dt: Double) {
        // Implementación por defecto: matriz de identidad (modelo de velocidad constante)
        // Las subclases deben sobrescribir esto para modelos más complejos
    }
    
    /**
     * Reinicia el filtro con un nuevo estado inicial.
     */
    fun reset(initialState: Matrix, initialCovariance: Matrix? = null) {
        require(initialState.rows == stateDimension && initialState.cols == 1) {
            "La dimensión del estado inicial debe ser $stateDimension x 1"
        }
        
        state = initialState.copy()
        covariance = initialCovariance?.copy() ?: Matrix.identity(stateDimension)
        lastUpdateTime = System.nanoTime()
    }
    
    // Getters para las matrices del filtro
    fun getState(): Matrix = state.copy()
    fun getCovariance(): Matrix = covariance.copy()
    fun getStateTransition(): Matrix = stateTransition.copy()
    fun getMeasurementMatrix(): Matrix = measurementMatrix.copy()
    fun getProcessNoiseCov(): Matrix = processNoiseCov.copy()
    fun getMeasurementNoiseCov(): Matrix = measurementNoiseCov.copy()
    
    // Setters para las matrices del filtro
    fun setState(newState: Matrix) {
        require(newState.rows == stateDimension && newState.cols == 1) {
            "La dimensión del estado debe ser $stateDimension x 1"
        }
        state = newState.copy()
    }
    
    fun setCovariance(newCovariance: Matrix) {
        require(newCovariance.rows == stateDimension && newCovariance.cols == stateDimension) {
            "La dimensión de la covarianza debe ser $stateDimension x $stateDimension"
        }
        covariance = newCovariance.copy()
    }
    
    fun setStateTransition(newStateTransition: Matrix) {
        require(newStateTransition.rows == stateDimension && newStateTransition.cols == stateDimension) {
            "La dimensión de la matriz de transición debe ser $stateDimension x $stateDimension"
        }
        stateTransition = newStateTransition.copy()
    }
    
    fun setControlMatrix(newControlMatrix: Matrix) {
        require(controlDimension > 0) { "El filtro no fue configurado para usar control" }
        require(newControlMatrix.rows == stateDimension && newControlMatrix.cols == controlDimension) {
            "La dimensión de la matriz de control debe ser $stateDimension x $controlDimension"
        }
        controlMatrix = newControlMatrix.copy()
    }
    
    fun setMeasurementMatrix(newMeasurementMatrix: Matrix) {
        require(newMeasurementMatrix.rows == measurementDimension && newMeasurementMatrix.cols == stateDimension) {
            "La dimensión de la matriz de medición debe ser $measurementDimension x $stateDimension"
        }
        measurementMatrix = newMeasurementMatrix.copy()
    }
    
    fun setProcessNoiseCov(newProcessNoiseCov: Matrix) {
        require(newProcessNoiseCov.rows == stateDimension && newProcessNoiseCov.cols == stateDimension) {
            "La dimensión de la covarianza del proceso debe ser $stateDimension x $stateDimension"
        }
        processNoiseCov = newProcessNoiseCov.copy()
    }
    
    fun setMeasurementNoiseCov(newMeasurementNoiseCov: Matrix) {
        require(newMeasurementNoiseCov.rows == measurementDimension && newMeasurementNoiseCov.cols == measurementDimension) {
            "La dimensión de la covarianza de medición debe ser $measurementDimension x $measurementDimension"
        }
        measurementNoiseCov = newMeasurementNoiseCov.copy()
    }
    
    /**
     * Clase para representar matrices y operaciones matriciales básicas.
     */
    class Matrix(val rows: Int, val cols: Int) {
        private val data: Array<DoubleArray>
        
        init {
            require(rows > 0 && cols > 0) { "Las dimensiones de la matriz deben ser positivas" }
            data = Array(rows) { DoubleArray(cols) }
        }
        
        /**
         * Establece el valor en la posición (i, j).
         */
        operator fun set(i: Int, j: Int, value: Double) {
            require(i in 0 until rows && j in 0 until cols) { "Índices fuera de rango" }
            data[i][j] = value
        }
        
        /**
         * Obtiene el valor en la posición (i, j).
         */
        operator fun get(i: Int, j: Int): Double {
            require(i in 0 until rows && j in 0 until cols) { "Índices fuera de rango" }
            return data[i][j]
        }
        
        /**
         * Crea una copia de esta matriz.
         */
        fun copy(): Matrix {
            val result = Matrix(rows, cols)
            for (i in 0 until rows) {
                System.arraycopy(data[i], 0, result.data[i], 0, cols)
            }
            return result
        }
        
        /**
         * Establece esta matriz como una copia de otra.
         */
        fun setTo(other: Matrix) {
            require(rows == other.rows && cols == other.cols) { "Las dimensiones de las matrices no coinciden" }
            for (i in 0 until rows) {
                System.arraycopy(other.data[i], 0, data[i], 0, cols)
            }
        }
        
        /**
         * Establece esta matriz como el producto de otras dos matrices.
         */
        fun setToProduct(a: Matrix, b: Matrix) {
            require(a.cols == b.rows) { "Las dimensiones de las matrices no son compatibles para la multiplicación" }
            require(rows == a.rows && cols == b.cols) { "La matriz de resultado tiene dimensiones incorrectas" }
            
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    var sum = 0.0
                    for (k in 0 until a.cols) {
                        sum += a[i, k] * b[k, j]
                    }
                    data[i][j] = sum
                }
            }
        }
        
        /**
         * Devuelve la matriz transpuesta.
         */
        fun transposed(): Matrix {
            val result = Matrix(cols, rows)
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    result[j, i] = this[i, j]
                }
            }
            return result
        }
        
        /**
         * Calcula la matriz inversa usando eliminación gaussiana.
         */
        fun inverse(): Matrix {
            require(rows == cols) { "Solo matrices cuadradas pueden ser invertidas" }
            
            val n = rows
            val a = Array(n) { DoubleArray(2 * n) }
            
            // Construir matriz aumentada [A | I]
            for (i in 0 until n) {
                for (j in 0 until n) {
                    a[i][j] = this[i, j]
                    a[i][j + n] = if (i == j) 1.0 else 0.0
                }
            }
            
            // Aplicar eliminación gaussiana
            for (i in 0 until n) {
                // Buscar el pivote máximo en la columna actual
                var maxRow = i
                for (k in i + 1 until n) {
                    if (Math.abs(a[k][i]) > Math.abs(a[maxRow][i])) {
                        maxRow = k
                    }
                }
                
                // Intercambiar filas si es necesario
                if (maxRow != i) {
                    val temp = a[i]
                    a[i] = a[maxRow]
                    a[maxRow] = temp
                }
                
                // Verificar si la matriz es singular
                if (Math.abs(a[i][i]) < 1e-10) {
                    throw ArithmeticException("La matriz es singular o casi singular")
                }
                
                // Escalonar la fila actual
                val pivot = a[i][i]
                for (j in i until 2 * n) {
                    a[i][j] /= pivot
                }
                
                // Eliminar otros elementos de la columna
                for (k in 0 until n) {
                    if (k != i && a[k][i] != 0.0) {
                        val factor = a[k][i]
                        for (j in i until 2 * n) {
                            a[k][j] -= a[i][j] * factor
                        }
                    }
                }
            }
            
            // Extraer la matriz inversa
            val result = Matrix(n, n)
            for (i in 0 until n) {
                for (j in 0 until n) {
                    result[i, j] = a[i][j + n]
                }
            }
            
            return result
        }
        
        // Operadores aritméticos
        operator fun plus(other: Matrix): Matrix {
            require(rows == other.rows && cols == other.cols) { "Las dimensiones de las matrices no coinciden" }
            val result = Matrix(rows, cols)
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    result[i, j] = this[i, j] + other[i, j]
                }
            }
            return result
        }
        
        operator fun minus(other: Matrix): Matrix {
            require(rows == other.rows && cols == other.cols) { "Las dimensiones de las matrices no coinciden" }
            val result = Matrix(rows, cols)
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    result[i, j] = this[i, j] - other[i, j]
                }
            }
            return result
        }
        
        operator fun times(scalar: Double): Matrix {
            val result = Matrix(rows, cols)
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    result[i, j] = this[i, j] * scalar
                }
            }
            return result
        }
        
        operator fun times(other: Matrix): Matrix {
            require(cols == other.rows) { "Las dimensiones de las matrices no son compatibles para la multiplicación" }
            val result = Matrix(rows, other.cols)
            for (i in 0 until rows) {
                for (j in 0 until other.cols) {
                    var sum = 0.0
                    for (k in 0 until cols) {
                        sum += this[i, k] * other[k, j]
                    }
                    result[i, j] = sum
                }
            }
            return result
        }
        
        operator fun plusAssign(other: Matrix) {
            require(rows == other.rows && cols == other.cols) { "Las dimensiones de las matrices no coinciden" }
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    data[i][j] += other[i, j]
                }
            }
        }
        
        operator fun minusAssign(other: Matrix) {
            require(rows == other.rows && cols == other.cols) { "Las dimensiones de las matrices no coinciden" }
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    data[i][j] -= other[i, j]
                }
            }
        }
        
        operator fun timesAssign(scalar: Double) {
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    data[i][j] *= scalar
                }
            }
        }
        
        // Métodos estáticos de fábrica
        companion object {
            /**
             * Crea una matriz de ceros.
             */
            fun zero(rows: Int, cols: Int): Matrix {
                return Matrix(rows, cols)
            }
            
            /**
             * Crea una matriz identidad.
             */
            fun identity(size: Int): Matrix {
                val result = Matrix(size, size)
                for (i in 0 until size) {
                    result[i, i] = 1.0
                }
                return result
            }
            
            /**
             * Crea una matriz a partir de un array 2D.
             */
            fun fromArray(array: Array<DoubleArray>): Matrix {
                require(array.isNotEmpty() && array[0].isNotEmpty()) { "El array no puede estar vacío" }
                val rows = array.size
                val cols = array[0].size
                val result = Matrix(rows, cols)
                for (i in 0 until rows) {
                    require(array[i].size == cols) { "Todas las filas deben tener la misma longitud" }
                    for (j in 0 until cols) {
                        result[i, j] = array[i][j]
                    }
                }
                return result
            }
            
            /**
             * Crea una matriz columna a partir de un array 1D.
             */
            fun columnVector(vararg values: Double): Matrix {
                val result = Matrix(values.size, 1)
                for (i in values.indices) {
                    result[i, 0] = values[i]
                }
                return result
            }
        }
        
        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("[")
            for (i in 0 until rows) {
                if (i > 0) sb.append(" ")
                sb.append("[")
                for (j in 0 until cols) {
                    sb.append(String.format("%.6f", data[i][j]))
                    if (j < cols - 1) sb.append(", ")
                }
                sb.append("]")
                if (i < rows - 1) sb.append(",\n")
            }
            sb.append("]")
            return sb.toString()
        }
    }
}
