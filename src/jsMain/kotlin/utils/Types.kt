package utils

typealias StoredFunction<R, T> = R.() -> T
typealias StoredAction = StoredFunction<Unit, Unit>

