/*
 * Copyright 2013 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc.interpolation

/**
 * Value as a part of SQL syntax.
 *
 * This value won't be treated as a binding parameter but will be appended as a part of SQL.
 *
 * Note: The constructor should NOT be used by library users at the considerable risk of SQL injection vulnerability.
 * https://github.com/scalikejdbc/scalikejdbc/issues/116
 */
class SQLSyntax private[scalikejdbc] (val value: String, val parameters: Seq[Any] = Vector()) {
  import Implicits._
  import SQLSyntax._

  override def equals(that: Any): Boolean = {
    if (that.isInstanceOf[SQLSyntax]) {
      val thatSqls = that.asInstanceOf[SQLSyntax]
      value == thatSqls.value && parameters == thatSqls.parameters
    } else {
      false
    }
  }

  override def toString(): String = s"SQLSyntax(value: ${value}, parameters: ${parameters})"

  def append(syntax: SQLSyntax) = sqls"${this} ${syntax}"

  def groupBy(columns: SQLSyntax*) = sqls"${this} group by ${csv(columns: _*)}"
  def having(condition: SQLSyntax) = sqls"${this} having ${condition}"

  def orderBy(columns: SQLSyntax*) = sqls"${this} order by ${csv(columns: _*)}"
  def asc = sqls"${this} asc"
  def desc = sqls"${this} desc"

  def limit(n: Int) = sqls"${this} limit ${SQLSyntax(n.toString)}"
  def offset(n: Int) = sqls"${this} offset ${SQLSyntax(n.toString)}"

  def where = sqls"${this} where"
  def where(where: SQLSyntax) = sqls"${this} where ${where}"

  def and = sqls"${this} and"
  def or = sqls"${this} or"

  def eq(column: SQLSyntax, value: Any) = sqls"${this} ${column} = ${value}"
  def ne(column: SQLSyntax, value: Any) = sqls"${this} ${column} <> ${value}"
  def gt(column: SQLSyntax, value: Any) = sqls"${this} ${column} > ${value}"
  def ge(column: SQLSyntax, value: Any) = sqls"${this} ${column} >= ${value}"
  def lt(column: SQLSyntax, value: Any) = sqls"${this} ${column} < ${value}"
  def le(column: SQLSyntax, value: Any) = sqls"${this} ${column} <= ${value}"

  def isNull(column: SQLSyntax) = sqls"${this} ${column} is null"
  def isNotNull(column: SQLSyntax) = sqls"${this} ${column} is not null"
  @deprecated("use between(column: SQLSyntax, a: Any, b: Any) instead of this", "1.6.2")
  def between(a: Any, b: Any) = sqls"${this} between ${a} and ${b}"
  def between(column: SQLSyntax, a: Any, b: Any) = sqls"${this} ${column} between ${a} and ${b}"

  def in(column: SQLSyntax, values: Seq[Any]) = sqls"${this} ${column} in (${values})"
  def notIn(column: SQLSyntax, values: Seq[Any]) = sqls"${this} ${column} not in (${values})"

  def like(column: SQLSyntax, value: String) = sqls"${this} ${column} like ${value}"
  def notLike(column: SQLSyntax, value: String) = sqls"${this} ${column} not like ${value}"

}

/**
 * A trait which has #resultAll: SQLSyntax
 */
trait ResultAllProvider {
  def resultAll: SQLSyntax
}

/**
 * A trait which has #asterisk: SQLSyntax
 */
trait AsteriskProvider {
  def asterisk: SQLSyntax
}

/*
 * SQLSyntax companion object
 */
object SQLSyntax {

  // #apply method should NOT be used by library users at the considerable risk of SQL injection vulnerability.
  // https://github.com/scalikejdbc/scalikejdbc/issues/116
  private[scalikejdbc] def apply(value: String, parameters: Seq[Any] = Nil) = new SQLSyntax(value, parameters)

  def unapply(syntax: SQLSyntax): Option[(String, Seq[Any])] = Some((syntax.value, syntax.parameters))

  import Implicits._

  def join(parts: Seq[SQLSyntax], delimiter: SQLSyntax): SQLSyntax = parts.foldLeft(sqls"") {
    case (sql, part) if !sql.isEmpty && !part.isEmpty => sqls"${sql} ${delimiter} ${part}"
    case (sql, part) if sql.isEmpty && !part.isEmpty => part
    case (sql, _) => sql
  }
  def csv(parts: SQLSyntax*): SQLSyntax = join(parts, sqls",")

  private[this] def hasAndOr(s: SQLSyntax): Boolean = {
    val statement = s.value.toLowerCase
    statement.matches(".+\\s+and\\s+.+") ||
      statement.matches(".+\\s+or\\s+.+")
  }

  def joinWithAnd(parts: SQLSyntax*): SQLSyntax = join(parts.map(p => if (hasAndOr(p)) sqls"(${p})" else p), sqls"and")
  def joinWithOr(parts: SQLSyntax*): SQLSyntax = join(parts.map(p => if (hasAndOr(p)) sqls"(${p})" else p), sqls"or")

  def groupBy(columns: SQLSyntax*) = sqls"".groupBy(columns: _*)
  def having(condition: SQLSyntax) = sqls"".having(condition)

  def orderBy(columns: SQLSyntax*) = sqls"".orderBy(columns: _*)
  def asc = sqls"".asc
  def desc = sqls"".desc

  def limit(n: Int) = sqls"".limit(n)
  def offset(n: Int) = sqls"".offset(n)

  def where = sqls"".where
  def where(where: SQLSyntax) = sqls"".where(where)

  def eq(column: SQLSyntax, value: Any) = sqls"".eq(column, value)
  def ne(column: SQLSyntax, value: Any) = sqls"".ne(column, value)
  def gt(column: SQLSyntax, value: Any) = sqls"".gt(column, value)
  def ge(column: SQLSyntax, value: Any) = sqls"".ge(column, value)
  def lt(column: SQLSyntax, value: Any) = sqls"".lt(column, value)
  def le(column: SQLSyntax, value: Any) = sqls"".le(column, value)

  def isNull(column: SQLSyntax) = sqls"".isNull(column)
  def isNotNull(column: SQLSyntax) = sqls"".isNotNull(column)
  @deprecated("use between(column: SQLSyntax, a: Any, b: Any) instead of this", "1.6.2")
  def between(a: Any, b: Any) = sqls"".between(a, b)
  def between(column: SQLSyntax, a: Any, b: Any) = sqls"".between(column, a, b)

  def in(column: SQLSyntax, values: Seq[Any]) = sqls"".in(column, values)
  def notIn(column: SQLSyntax, values: Seq[Any]) = sqls"".notIn(column, values)

  def like(column: SQLSyntax, value: String) = sqls"".like(column, value)
  def notLike(column: SQLSyntax, value: String) = sqls"".notLike(column, value)

  def distinct(column: SQLSyntax) = sqls"distinct ${column}"

  def avg(column: SQLSyntax) = sqls"avg(${column})"

  def count = sqls"count(1)"
  def count(column: SQLSyntax) = sqls"count(${column})"
  def count(asteriskProvider: AsteriskProvider) = sqls"count(${asteriskProvider.asterisk})"

  def min(column: SQLSyntax) = sqls"min(${column})"
  def max(column: SQLSyntax) = sqls"max(${column})"
  def sum(column: SQLSyntax) = sqls"sum(${column})"

  def abs(column: SQLSyntax) = sqls"abs(${column})"
  def floor(column: SQLSyntax) = sqls"floor(${column})"
  def ceil(column: SQLSyntax) = sqls"ceil(${column})"
  def ceiling(column: SQLSyntax) = sqls"ceiling(${column})"

  def ? = sqls"?"

  def currentDate = sqls"current_date"
  def currentTimestamp = sqls"current_timestamp"
  def dual = sqls"dual"

  /**
   * Rerturns an optional SQLSyntax which is flatten (from option array) and joined with 'and'.
   *
   * {{{
   *   val cond: Option[SQLSyntax] = SQLSyntax.toAndConditionOpt(Some(sqls"id = $id"), None, Some(sqls"name = $name"))
   *   cond.get.statement // "id = ? or (name = ? or name is null)"
   *   cond.get.parameters // Seq(123, "Alice")
   * }}}
   */
  def toAndConditionOpt(conditions: Option[SQLSyntax]*): Option[SQLSyntax] = {
    val cs: Seq[SQLSyntax] = conditions.flatten
    if (cs.isEmpty) None else Some(joinWithAnd(cs: _*))
  }

  /**
   * Rerturns an optional SQLSyntax which is flatten (from option array) and joined with 'or'.
   *
   * {{{
   *   val cond: Option[SQLSyntax] = SQLSyntax.toOrConditionOpt(Some(sqls"id = $id"), None, Some(sqls"name = $name"))
   *   cond.get.statement // "id = ? or (name = ? or name is null)"
   *   cond.get.parameters // Seq(123, "Alice")
   */
  def toOrConditionOpt(conditions: Option[SQLSyntax]*): Option[SQLSyntax] = {
    val cs: Seq[SQLSyntax] = conditions.flatten
    if (cs.isEmpty) None else Some(joinWithOr(cs: _*))
  }

}

