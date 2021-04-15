package endpoint.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class Model(id: Id)

object Model {
  implicit val multiFooEncoder: JsonEncoder[Model] = DeriveJsonEncoder.gen
  implicit val multiFooDecoder: JsonDecoder[Model] = DeriveJsonDecoder.gen
}

final case class Id(value: String) extends AnyVal

object Id {
  implicit val idEncoder: JsonEncoder[Id] = DeriveJsonEncoder.gen
  implicit val idDecoder: JsonDecoder[Id] = DeriveJsonDecoder.gen
}
