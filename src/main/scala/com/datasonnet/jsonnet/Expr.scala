package com.datasonnet.jsonnet

/*-
 * Copyright 2019-2022 the original author or authors.
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

import scala.collection.{BitSet, mutable}
/**
  * [[Expr]]s are the parsed syntax trees of a Jsonnet program. They model the
  * program mostly as-written, except for resolving local variable names and
  * assigning them indices in the scope bindings array.
  *
  * Each [[Expr]] represents an expression in the Jsonnet program, and contains an
  * integer offset into the file that is later used to provide error messages.
  */
sealed trait Expr{
  def offset: Int
}
object Expr{
  case class Null(offset: Int) extends Expr
  case class True(offset: Int) extends Expr
  case class False(offset: Int) extends Expr
  case class Self(offset: Int) extends Expr
  case class Super(offset: Int) extends Expr
  case class $(offset: Int) extends Expr

  case class Str(offset: Int, value: String) extends Expr
  case class Num(offset: Int, value: Double) extends Expr
  case class Id(offset: Int, value: Int) extends Expr
  case class Arr(offset: Int, value: Seq[Expr]) extends Expr
  case class Obj(offset: Int, value: ObjBody) extends Expr

  sealed trait FieldName

  object FieldName{
    case class Fixed(value: String) extends FieldName
    case class Dyn(expr: Expr) extends FieldName
  }
  sealed trait Member

  object Member{
    sealed trait Visibility
    object Visibility{

      object Normal extends Visibility
      object Hidden extends Visibility
      object Unhide extends Visibility
    }
    case class Field(offset: Int,
                     fieldName: FieldName,
                     plus: Boolean,
                     args: Option[Params],
                     sep: Visibility,
                     rhs: Expr) extends Member
    case class BindStmt(value: Bind) extends Member
    case class AssertStmt(value: Expr, msg: Option[Expr]) extends Member
  }


  case class Parened(offset: Int, value: Expr) extends Expr
  case class Params(args: IndexedSeq[(String, Option[Expr], Int)]){
    val argIndices: Map[String, Int] = args.map{case (k, d, i) => (k, i)}.toMap
    val noDefaultIndices: BitSet = mutable.BitSet.empty ++ args.collect{case (_, None, i) => i}
    val defaults: IndexedSeq[(Int, Expr)] = args.collect{case (_, Some(x), i) => (i, x)}
    val allIndices: Set[Int] = args.map{case (_, _, i) => i}.toSet
  }
  case class Args(args: Seq[(Option[String], Expr)])

  case class UnaryOp(offset: Int, op: UnaryOp.Op, value: Expr) extends Expr
  object UnaryOp{
    sealed trait Op
    case object `+` extends Op
    case object `-` extends Op
    case object `~` extends Op
    case object `!` extends Op
  }
  case class BinaryOp(offset: Int, lhs: Expr, op: BinaryOp.Op, rhs: Expr) extends Expr
  object BinaryOp{
    sealed trait Op
    case object `*` extends Op
    case object `/` extends Op
    case object `%` extends Op
    case object `+` extends Op
    case object `-` extends Op
    case object `<<` extends Op
    case object `>>` extends Op
    case object `<` extends Op
    case object `>` extends Op
    case object `<=` extends Op
    case object `>=` extends Op
    case object `in` extends Op
    case object `==` extends Op
    case object `!=` extends Op
    case object `&` extends Op
    case object `^` extends Op
    case object `|` extends Op
    case object `&&` extends Op
    case object `||` extends Op
  }
  case class AssertExpr(offset: Int, asserted: Member.AssertStmt, returned: Expr) extends Expr
  case class LocalExpr(offset: Int, bindings: Seq[Bind], returned: Expr) extends Expr

  case class Bind(offset: Int, name: Int, args: Option[Params], rhs: Expr)
  case class Import(offset: Int, value: String) extends Expr
  case class ImportStr(offset: Int, value: String) extends Expr
  case class Error(offset: Int, value: Expr) extends Expr
  case class Apply(offset: Int, value: Expr, args: Args) extends Expr
  case class Select(offset: Int, value: Expr, name: String) extends Expr
  case class Lookup(offset: Int, value: Expr, index: Expr) extends Expr
  case class Slice(offset: Int,
                   value: Expr,
                   start: Option[Expr],
                   end: Option[Expr],
                   stride: Option[Expr]) extends Expr
  case class Function(offset: Int, params: Params, body: Expr) extends Expr
  case class IfElse(offset: Int, cond: Expr, then: Expr, `else`: Option[Expr]) extends Expr

  sealed trait CompSpec extends Expr
  case class IfSpec(offset: Int, cond: Expr) extends CompSpec
  case class ForSpec(offset: Int, name: Int, cond: Expr) extends CompSpec

  case class Comp(offset: Int, value: Expr, first: ForSpec, rest: Seq[CompSpec]) extends Expr
  case class ObjExtend(offset: Int, base: Expr, ext: ObjBody) extends Expr

  sealed trait ObjBody
  object ObjBody{
    case class MemberList(value: Seq[Member]) extends ObjBody
    case class ObjComp(preLocals: Seq[Member.BindStmt],
                       key: Expr,
                       value: Expr,
                       postLocals: Seq[Member.BindStmt],
                       first: ForSpec,
                       rest: Seq[CompSpec]) extends ObjBody
  }

}