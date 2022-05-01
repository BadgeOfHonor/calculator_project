package calculator

import calculator.BigNumber.Companion.div
import calculator.BigNumber.Companion.minus
import calculator.BigNumber.Companion.plus
import calculator.BigNumber.Companion.times
import calculator.BigNumber.Companion.toBigNumber
import calculator.Calculator.CalcBoard.assignValue
import calculator.Calculator.CalcBoard.assignVariable
import calculator.Calculator.CalcBoard.clearSpace
import calculator.Calculator.CalcBoard.getValue
import calculator.Calculator.CalcBoard.isExist
import calculator.Calculator.Cleaning.clearAndFixExpres
import calculator.Calculator.ManagerComputer.calculateInfix
import calculator.Calculator.ManagerComputer.calculateRPN
import calculator.Calculator.ManagerComputer.createListVariable
import calculator.Calculator.ManagerComputer.createOperationList
import calculator.Calculator.ManagerComputer.substituteVariables
import calculator.Calculator.Validation.isExpression
import calculator.Calculator.Validation.isNormalParenthesis
import calculator.Calculator.Validation.isTrueListOperation
import calculator.Calculator.Validation.isTrueListVariable
import calculator.Calculator.Validation.isValue
import calculator.Calculator.Validation.isVariable
import calculator.Calculator.Validation.isVariableWithSing


class Calculator {
    var modeRPN = false
    var repeatingsignsMathOpera = false

    init { run() }

    fun run() {
        while (true) {
            val userInput = readln().trim()
            when {
                userInput.matches(Rules.Command.regex) -> {
                    val userCommand = userInput.lowercase().substringAfter('/')
                    when (userCommand) {
                        "exit" -> { println(M.Bye.msg); return }
                        "help" -> { println(M.Help.msg) }
                        "rpn" -> { modeRPN = true; println(M.Postfix.msg)}
                        "infix" -> { modeRPN = false; println(M.Infix.msg) }
                        "repeat-on" -> { repeatingsignsMathOpera = true; println(M.RepeatOperaOn.msg)}
                        "repeat-off" -> { repeatingsignsMathOpera = false; println(M.RepeatOperaOff.msg)}
                        else -> { println(M.Uncommand.msg) }
                    }
                }
                userInput.matches(Rules.Expression.regex) -> {
                    println(try { mathExpres(userInput) } catch (e: Exception) { println(e.message); continue })
                }
                userInput.matches(Rules.Assignmention.regex) -> {
                    val assignmentList = userInput.split(Rules.Equally.regex).map { it.trim() }.toMutableList()
                    if (assignmentList.size > 2) { println(M.InvAss.msg); continue }
                    val identifier = assignmentList.first()
                    if (!identifier.isVariable()) { println(M.InvIdent.msg); continue }
                    val value = assignmentList.last()
                    when {
                        value.isValue() -> { identifier.assignValue(value); continue }
                        value.isVariable() -> if (!identifier.assignVariable(value)) println(M.UnVar.msg)
                        value.isExpression() -> {
                            val result = try { mathExpres(value) } catch (e: Exception) { println(e.message); continue }
                            identifier.assignValue(result)
                        }
                        else -> println(M.InvAss.msg)
                    }
                }
                else -> if (userInput.isNotEmpty()){ println(M.InvExpres.msg) }
            }
        }
    }
    fun mathExpres(expression: String): BigNumber {
        if (!repeatingsignsMathOpera && expression.contains(Rules.RepeatOperations.regex)) {
            throw Exception(M.InvExpres.msg)
        }
        if (!expression.isNormalParenthesis()) throw Exception(M.InvExpres.msg)
        val expres = expression.clearAndFixExpres()
        var operationList = expres.createOperationList()
        if (!operationList.isTrueListOperation()) throw Exception(M.UnOpera.msg)
        if (!operationList.isTrueListVariable()) throw Exception(M.UnVar.msg)
        operationList = operationList.substituteVariables()
        return if (modeRPN) operationList.calculateRPN() else operationList.calculateInfix()
    }

    object CalcBoard {
        var variableSpace = LinkedHashMap<String, BigNumber>()

        fun String.assignValue(value: String) {
            this.assignValue(value.toBigNumber())
        }
        fun String.assignValue(value: BigNumber) {
            if (this.isVariableWithSing()) {
                val sign = this.first()
                val identifier = this.substring(1)
                if (sign == '-') { variableSpace[identifier] = (-1).toBigNumber() * value } else variableSpace[identifier] = value
            } else {
                variableSpace[this] = value
            }
        }
        fun String.assignVariable(variable: String): Boolean {
            if (variable.isExist()) this.assignValue(variableSpace[variable]!!)
            return variable.isExist()
        }
        fun String.isExist(): Boolean {
            val sign = this.first()
            val identifier = if (sign == '+' || sign == '-') this.substring(1) else this
            return variableSpace[identifier] != null
        }
        fun String.getValue(): String {
            return variableSpace[this]!!.toString()
        }
        fun MutableMap<String, String>.clearSpace() {
            variableSpace -= this.keys
        }
    }

    object Cleaning {
        fun String.clearAndFixExpres(): String {
            var expres = this
            val separation = { i: Int ->
                expres = expres.substring(0, i + 1) + " " + expres.substring(i + 1)
            }
            val replacement = { i: Int, ch: Char ->
                expres = expres.substring(0, i) + ch + expres.substring(i + 2)
            }
            val removement = { i: Int ->
                expres = expres.substring(0, i) + expres.substring(i + 2)
            }
            var i = 0
            while (i != expres.length - 1) {
                val str: String = expres[i].toString() + expres[i + 1]
                when {
                    str.matches(Rules.OpAndOp.regex) -> {
                        if (str.first() != str.last()) {
                            separation(i); i += 2
                        } else {
                            when (str.first()) {
                                '(', ')' -> {
                                    separation(i); i += 2
                                }
                                '-' -> if (expres[i + 2] == '-') removement(i) else replacement(i, '+')
                                else -> replacement(i, str.first())
                            }
                        }
                    }
                    str.matches(Rules.VarOrValAndOp.regex) -> {
                        separation(i); i += 2
                    }
                    str.matches(Rules.OpAndVarOrVal.regex) && i >= 2 -> {
                        if (expres.substring(i - 2, i).matches(Rules.OpOutParenthesisRightAndSp.regex)) {
                            when(str.first()) {
                                '+' -> { replacement(i, str.last()); i-- }
                                '-' -> { i++; continue }
                            }
                        }
                        separation(i); i += 2
                    }
                    else -> i++
                }
            }
            return expres.replace(Regex("\\h\\h+"), " ")
        }
    }

    object Validation {
        fun String.isValue(): Boolean = this.matches(Rules.ValueSign.regex)
        fun String.isVariable(): Boolean = this.matches(Rules.Variables.regex)
        fun String.isExpression(): Boolean = this.matches(Rules.Expression.regex)
        fun String.isVariableWithSing(): Boolean = this.matches(Rules.VariableWithSign.regex)
        fun String.isParenthesisExist(): Boolean = this.any { it == '(' || it == ')'}
        fun String.isNormalParenthesis(): Boolean {
            return if (this.isParenthesisExist()) {
                val parenthesisList = this.filter { it == '(' || it == ')' }
                if (parenthesisList.count { it == '('} != parenthesisList.count { it == ')'}) false else {
                    var status = true
                    var c = 0
                    parenthesisList.forEach {
                        when (it) {
                            '(' -> c++
                            ')' -> c--
                        }
                        if (c < 0) { status = false; return@forEach  }
                    }
                    status
                }
            } else true
        }
        fun List<String>.isTrueListOperation(): Boolean {
            val listOperation = this.filterNot { it.matches(Rules.ValueSign.regex) || it.matches(Rules.Variables.regex) }
            var status = true
            listOperation.forEach { if (!it.matches(Rules.Operation.regex)) { status = false; return@forEach } }
            return status
        }
        fun List<String>.isTrueListVariable(): Boolean {
            val listVariable = this.createListVariable()
            var status = true
            listVariable.forEach { if (!it.isExist()) { status = false; return@forEach } }
            return status
        }
    }

    object ManagerComputer {
        var specialSymbol = "#1"
        var stackOperation = LinkedHashMap<String, String>()

        fun List<String>.calculateInfix(): BigNumber {
            val opTableByPriority = this.operationsTableByPriority()
            var result = 0.toBigNumber()
            for (item in opTableByPriority.keys) {
                result = opTableByPriority[item]!!.createOperationList().substituteVariables().compute()
                item.assignValue(result)
            }
            opTableByPriority.clearSpace()
            return result
        }
        fun List<String>.calculateRPN(): BigNumber = this.creatRPN().computeRPN()
        fun List<String>.operationsTableByPriority(): MutableMap<String, String> {
            stackOperation.clear()
            var operationList = this.toMutableList().also { it.add(0,"("); it.add( ")") }
            val stackIndex = mutableListOf<Int>()
            var i = 0
            while (i <= operationList.lastIndex) {
                when (operationList[i]) {
                    "(" -> stackIndex.add(i)
                    ")" -> {
                        val indexLastParenthesis = stackIndex.removeAt(stackIndex.lastIndex)

                        var subOperation = operationList
                            .slice(indexLastParenthesis + 1 until i).joinToString(" ")
                        operationList = operationList
                            .mapIndexedNotNull { index, s ->
                                if (index !in indexLastParenthesis + 1..i ) s else null
                            }.toMutableList()

                        val subOperationList = subOperation
                            .split(Rules.Op1stPriority.regex)
                            .mapNotNull { if (it.matches(Rules.OpExclude.regex)) null else it }
                        if (subOperationList.isNotEmpty()) {
                            for (j in subOperationList) {
                                stackOperation[specialSymbol] = j
                                subOperation = subOperation.replace(j, specialSymbol)
                                specialSymbol = specialSymbol.first().toString() + (specialSymbol.last().digitToInt() + 1)
                            }
                        }
                        stackOperation[specialSymbol] = subOperation
                        operationList[indexLastParenthesis] = specialSymbol
                        specialSymbol = specialSymbol.first().toString() + ( specialSymbol.last().digitToInt() + 1 )
                        i = indexLastParenthesis
                    }
                    else -> {}
                }
                i++
            }
            return stackOperation
        }
        fun String.createOperationList(): List<String> = this.split(" ").filter { it.isNotEmpty() }
        fun List<String>.createListVariable(): List<String> {
            return this.filterNot { it.matches(Rules.ValueSign.regex) || it.matches(Rules.Operation.regex) }
        }
        fun List<String>.substituteVariables():List<String> {
            val listVariable = this.createListVariable()
            val operationList = this.toMutableList()
            operationList.replaceAll { if (it in listVariable) it.getValue() else it }
            return operationList
        }
        fun List<String>.computeRPN(): BigNumber {
            val result = ArrayDeque<String>()
            for (i in this) {
                if (i.matches(Rules.OperationOutParenthesis.regex)) {
                    val operand2 = result.removeLast()
                    val operationList = ArrayDeque<String>()
                    operationList.addLast(result.removeLast())
                    operationList.addLast(i)
                    operationList.addLast(operand2)
                    result.addLast(operationList.compute().toString())
                } else result.addLast(i)
            }
            return result.first().toBigNumber()
        }
        fun List<String>.creatRPN(): List<String> {
            val result = ArrayDeque<String>()
            val stackOp = ArrayDeque<String>()
            var isBreak = true

            val reloadOperation: () -> Unit = {
                for (j in stackOp.lastIndex downTo 0) {
                    if (stackOp[j] == "(") {
                        if (isBreak) {
                            stackOp.removeLast(); break
                        } else {
                            stackOp.removeLast(); continue
                        }
                    }
                    result.addLast(stackOp.removeLast())
                }
            }
            for (i in this) when {
                i == "(" -> {
                    stackOp.addLast(i)
                }
                i == ")" -> {
                    reloadOperation()
                }
                i.matches(Rules.OperationOutParenthesis.regex) -> {
                    when {
                        stackOp.isEmpty() -> {
                            stackOp.addLast(i)
                        }
                        MathOpera.comparePriority(i, stackOp.last()) -> {
                            stackOp.addLast(i)
                        }
                        else -> {
                            reloadOperation()
                            stackOp.addLast(i)
                        }
                    }
                }
                i == this.last() -> {
                    isBreak = false
                    if (stackOp.isEmpty()) {
                        result.addLast(i)
                        return result
                    }
                    result.addLast(i)
                }
                else -> {
                    result.addLast(i)
                }
            }
            reloadOperation()
            return result
        }
        fun List<String>.compute(): BigNumber {
            var result = this.first().toBigNumber()
            for (i in 1..this.size - 2 step 2) {
                val operation = MathOpera.findOp(this[i].first())
                result = operation(result, this[i + 1].toBigNumber())
            }
            return result
        }
    }
}

enum class MathOpera(val char: Char, val op: (BigNumber, BigNumber) -> BigNumber, val priority: Int)  {
    Plus('+', { a: BigNumber, b: BigNumber -> a + b}, 1),
    Minus('-', { a: calculator.BigNumber, b: BigNumber -> a - b}, 1),
    Multiplication('*', { a: BigNumber, b: BigNumber -> a * b}, 2),
    Division('/', { a: BigNumber, b: BigNumber -> a / b}, 2),
    Degree('^', { a: BigNumber, b: BigNumber -> a.bigPow(b) }, 3),
    NULL(' ',{ _: BigNumber, _: BigNumber -> "0".toBigNumber() }, 0);

    companion object {

        fun comparePriority(o1: String, o2: String) = priority(o1) - priority(o2) > 0

        fun priority(o: String): Int {
            for (i in values()) {
                if (o.first() == i.char) return i.priority
            }
            return NULL.priority
        }

        fun findOp(char: Char): (BigNumber, BigNumber) -> BigNumber {
            for (i in values()) {
                if (char == i.char) return i.op
            }
            return NULL.op
        }
    }
}

enum class M(val msg: String) {
    Help("The program calculates simple mathematical expressions containing the operations:\n\n" +
            "+ - addition:\n" +
            "- - subtraction;\n" +
            "* - multiplication;\n" +
            "/ - division;\n" +
            "^ - exponentiation.\n\n" +
            "The program supports management commands:\n\n" +
            "/exit - exit from program\n" +
            "/rpn - Postfix notation mode is activated\n" +
            "/infix - Infix notation mode is activated\n" +
            "/repeat-on - repeat mode of all mathematical operations is activated\n" +
            "/repeat-off - repeat mode of all mathematical operations is deactivated except +, -\n\n" +
            "by default - /infix, /repeat-off"),
    Bye("Bye!"),
    Uncommand("Unknown command"),
    InvAss("Invalid assignment"),
    InvIdent("Invalid identifier"),
    UnVar("Unknown variable"),
    InvExpres("Invalid expression"),
    UnOpera("Unknown operation"),
    Postfix("Postfix notation mode is activated"),
    Infix("Infix notation mode is activated"),
    RepeatOperaOn("Repeat mode ON"),
    RepeatOperaOff("Repeat mode OFF")
}

enum class Rules(val lable: String, val regex: Regex = Regex(lable))  {
    Command("^/[\\w-]*"),
    Signs("(-|\\+)?"),
    Value("\\d+"),
    ValueParts("\\d"),
    ValueSign("(-|\\+)?\\d+"),
    Variable("[a-zA-Z]+"),
    VariableParts("[a-zA-Z]"),
    Variables("(-|\\+)?[a-zA-Z]+"),
    VariableWithSign("(-|\\+)[a-zA-Z]+"),
    Operation("(-|\\+|/|\\*|\\^|\\(|\\))"),
    OperationOutParenthesis("(-|\\+|/|\\*|\\^)"),
    Operations("(\\h*(-+|\\++|\\/+|\\*+|\\^+|\\(+|\\)+))"),
    RepeatOperations("(\\/\\/+|\\*\\*+|\\^\\^+)"),
    Equally("\\h*=\\h*"),
    ParenthesisRight("[)]"),
    ParenthesisLeft("[(]"),
    ParenthesisRights("((\\h*${ParenthesisRight.lable})+)?"),
    ParenthesisLefts("((\\h*${ParenthesisLeft.lable})+)?"),

    Expression("(${ParenthesisLefts.lable}(\\h*${Signs.lable}(${Variable.lable}|${Value.lable}))${ParenthesisRights.lable})(${ParenthesisRights.lable}${Operations.lable}${ParenthesisLefts.lable}(\\h*${Signs.lable}(${Variable.lable}|${Value.lable}))${ParenthesisRights.lable})*"),
    Assignmention("\\w+${Equally.lable}[\\w+\\d\\D\\W]+"),

    VarOrValAndOp("(${VariableParts.lable}|${ValueParts.lable})${Operation.lable}"),
    OpAndVarOrVal("${Operation.lable}(${VariableParts.lable}|${ValueParts.lable})"),
    OpAndOp("${Operation.lable}${Operation.lable}"),
    OpOutParenthesisRightAndSp("(-|\\+|/|\\*|\\^|\\()\\s"),

    Op1stPriority("\\s(-|\\+)\\s"),
    OpExclude("(-?|\\+?)\\d+|(-?|\\+?)[a-zA-Z]+|#\\d+"),
}

fun main() {
    Calculator()


}

//    verified
//
//    3 + 8 * ( ( 4 + 3 ) * 2 + 1 ) - 6 / ( 2 + 1 )
//    3+8*((4+3)*2+1)-6/(2+1)
//    3+++8***((4++3)****2+++1)---6/(2++1)
//    3+8****((4++++3) ** 2 +++++ 1)- 6 ////(   2 +1)
//    3+8****((4----3) **2     --- -1)   ---6   //// (   2 +1)




