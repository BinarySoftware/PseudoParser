package org.PseudoLang

import org.PseudoLang.syntax.text.ast.AST
import org.enso.syntax.text.ast.Repr
import org.enso.syntax.text.ast.Repr._

////////////////////////////////////////////////////////////////////////////////
//// Transpiler ////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
object Transpiler {
  def run(ast: AST):       String       = transpile(ast).build()
  def transpile(ast: AST): Repr.Builder = traverse(0, ast.elems)

  def traverse(indent: Int, stack: List[AST.Elem]): Repr.Builder = {
    stack match {
      case (f: AST.Func) :: rest =>
        f.block match {
          case b: AST.Block =>
            val fDecl = R + "def " + f.name + f.args + ":"
            R + fDecl + traverseBlock(b) + traverse(indent, rest)
          case _ => R + f.name + f.args + traverse(indent, rest)
        }
      case (n: AST.Newline) :: rest => R + n + indent + traverse(indent, rest)
      case (i: AST.If) :: rest =>
        val ifRepr = R + "if" + i.condition + ":"
        val bRepr = i.block match {
          case b: AST.Block => R + traverseBlock(b)
          case oth          => R + oth
        }
        R + ifRepr + bRepr + traverse(indent, rest)
      case (t: AST.If.ThenCase) :: rest =>
        val headRepr = t.e.head match {
          case _: AST.Spacing => R + traverse(indent, t.e.tail)
          case _              => R + traverse(indent, t.e)
        }
        R + headRepr + traverse(indent, rest)
      case (e: AST.If.ElseCase) :: rest =>
        val iRepr = e.e.head match {
          case i: AST.If =>
            val elRepr = R + "elif " + i.condition + ":"
            val bRepr = i.block match {
              case b: AST.Block => traverseBlock(b)
              case oth          => oth.repr
            }
            R + elRepr + bRepr + traverse(indent, e.e.tail)
          case _: AST.Spacing => R + "else: " + traverse(indent, e.e.tail)
          case _              => R + "else: " + traverse(indent, e.e)
        }
        R + iRepr + traverse(indent, rest)
      case (l: AST.While) :: rest =>
        val bRepr = l.block match {
          case b: AST.Block => R + traverseBlock(b)
          case oth          => R + oth
        }
        R + "while " + l.condition + ":" + bRepr + traverse(indent, rest)
      case (_: AST.Comment) :: rest => R + traverse(indent, rest)
      case undefined :: rest        => R + undefined.repr + traverse(indent, rest)
      case Nil                      => R
    }
  }

  def traverseBlock(b: AST.Block): Repr.Builder = {
    R + AST.Newline() + b.indent + traverse(b.indent, b.elems)
  }
}
