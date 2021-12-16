package org.openmole.tool.types

object TypeConverter {
  def apply[F, T](f: F ⇒ T): TypeConverter[F, T] = new TypeConverter[F, T] {
    def apply(x: F) = f(x)
  }

  implicit def fromStringIsTypeConverter[T](implicit f: FromString[T]): TypeConverter[String, T] = TypeConverter[String, T](f.apply)
  implicit def toDoubleIsTypeConverter[T](implicit f: ToDouble[T]): TypeConverter[T, Double] = TypeConverter[T, Double](f.apply)

}
trait TypeConverter[-F, +T] {
  def apply(f: F): T
}
