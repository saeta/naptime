/*
 * Copyright 2016 Coursera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.coursera.naptime

import java.lang.StringBuilder

import com.linkedin.data.schema.DataSchema
import com.linkedin.data.schema.Name
import com.linkedin.data.schema.PrimitiveDataSchema
import com.linkedin.data.schema.RecordDataSchema
import com.linkedin.data.schema.RecordDataSchema.RecordType
import com.linkedin.data.schema.StringDataSchema
import com.linkedin.data.schema.TyperefDataSchema
import com.typesafe.scalalogging.StrictLogging
import scala.collection.JavaConverters._

object Types extends StrictLogging {

  object Relations {
    val PROPERTY_NAME = "related"
  }

  @deprecated("Please use the one with fields included", "0.2.4")
  def computeAsymType(
    typeName: String,
    keyType: DataSchema,
    valueType: RecordDataSchema): RecordDataSchema = {
    if (keyType.hasError || valueType.hasError) {
      throw new RuntimeException(s"Input schemas have error: $keyType $valueType")
    }
    keyType match {
      case primitive: PrimitiveDataSchema =>
        computeAsymTypeWithPrimitiveKey(typeName, primitive, valueType)
      case record: RecordDataSchema =>
        computeAsymTypeWithRecordKey(typeName, record, valueType)
      case typeref: TyperefDataSchema =>
        ??? // TODO(saeta): handle typeref schemas
      case unknown: DataSchema =>
        throw new RuntimeException(s"Cannot compute asymmetric type for key type: $unknown")
    }
  }

  /**
   * Computes an asymmetric type from the schemas for the input types.
   *
   * TODO: extend to support server-computed fields!
   *
   * @param typeName The full name of the computed type.
   * @param keyType The schema for the key.
   * @param valueType The schema for the values.
   * @return The computed asymmetric type.
   * @throws RuntimeException if problems are encountered.
   */
  def computeAsymType(
      typeName: String,
      keyType: DataSchema,
      valueType: RecordDataSchema,
      fields: Fields[_]): RecordDataSchema = {
    if (keyType.hasError || valueType.hasError) {
      throw new RuntimeException(s"Input schemas have error: $keyType $valueType")
    }
    val mergedSchema = keyType match {
      case primitive: PrimitiveDataSchema =>
        computeAsymTypeWithPrimitiveKey(typeName, primitive, valueType)
      case record: RecordDataSchema =>
        computeAsymTypeWithRecordKey(typeName, record, valueType)
      case typeref: TyperefDataSchema =>
        ??? // TODO(saeta): handle typeref schemas
      case unknown: DataSchema =>
        throw new RuntimeException(s"Cannot compute asymmetric type for key type: $unknown")
    }
    for ((name, relation) <- fields.relations) {
      Option(mergedSchema.getField(name)) match {
        case None =>
          logger.warn(s"Fields for resource $typeName mentions field name '$name' but this field " +
            "is not found in the merged type schema.")
        case Some(field) =>
          val properties = field.getProperties.asScala
          val relatedMap = Map[String, AnyRef](
            Relations.PROPERTY_NAME -> relation.identifier)
          field.setProperties((properties ++ relatedMap).asJava)
      }
    }
    mergedSchema
  }

  /**
   * Merges a Record key type and a value [Record] type into a new asymmetric type.
   *
   * TODO: change the schemas / types of embedded key types to primitives.
   *
   * @param typeName The name of the asymmetric type to create.
   * @param keyType The schema for the key type of the naptime resource.
   * @param valueType The schema for the value type of the naptime resource.
   * @return The new asymmetric type.
   */
  private[this] def computeAsymTypeWithRecordKey(
      typeName: String,
      keyType: RecordDataSchema,
      valueType: RecordDataSchema): RecordDataSchema = {
    val errorMessageBuilder = new StringBuilder
    val recordDataSchema = new RecordDataSchema(new Name(typeName), RecordType.RECORD)
    val combinedFields = keyType.getFields().asScala.toList ++ valueType.getFields().asScala
    val fields = if (combinedFields.exists(_.getName == "id")) {
      combinedFields // The key type had an `id` field defined as part of it.
    } else {
      val idField = new RecordDataSchema.Field(new StringDataSchema)
      idField.setOptional(false)
      idField.setName("id", errorMessageBuilder)
      idField.setRecord(recordDataSchema)
      idField :: combinedFields
    }
    recordDataSchema.setFields(fields.asJava, errorMessageBuilder)
    if (errorMessageBuilder.length() > 0) {
      logger.warn(s"Error while computing asymmetric type $typeName: $errorMessageBuilder")
    }
    recordDataSchema
  }

  /**
   * Merges a primitive key type and a value [Record] type into a new asymmetric type.
   *
   * TODO: change the schemas / types of embedded key types to primitives.
   *
   * @param typeName THe name of the asymmetric type to create.
   * @param keyType The schema for the key type of the naptime resource.
   * @param valueType The schema for the value type of the naptime resource.
   * @return The new asymmetric type.
   */
  private[this] def computeAsymTypeWithPrimitiveKey(
      typeName: String,
      keyType: PrimitiveDataSchema,
      valueType: RecordDataSchema): RecordDataSchema = {
    val errorMessageBuilder = new StringBuilder
    val recordDataSchema = new RecordDataSchema(new Name(typeName), RecordType.RECORD)
    val idField = new RecordDataSchema.Field(keyType)
    idField.setOptional(false)
    idField.setName("id", errorMessageBuilder)
    idField.setRecord(recordDataSchema)
    val fields = idField :: valueType.getFields().asScala.toList
    recordDataSchema.setFields(fields.asJava, errorMessageBuilder)
    if (errorMessageBuilder.length() > 0) {
      logger.warn(s"Error while computing asymmetric type $typeName: $errorMessageBuilder")
    }
    recordDataSchema
  }
}
