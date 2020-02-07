package calculator

import java.math.BigInteger
import java.util.*
import kotlin.math.pow

private var variablesMap = mutableMapOf<String, BigInteger>()
private lateinit var input: String
private var inputsList = mutableListOf<String>()
private var stack = mutableListOf<String>()
private var queue = mutableListOf<String>()
private var expressionList = mutableListOf<String>()

fun main() {
    val scanner = Scanner(System.`in`)
    loop@ while (true) {
        initLoop(scanner)
        if (inputsList[0].isEmpty() || input.first() == '/') {
            when (inputsList[0]) {
                "" -> continue@loop
                "/exit" -> {
                    println("Bye!")
                    break@loop
                }
                "/help" -> println("The program calculates the sum of numbers")
                else -> println("Unknown command")
            }
        } else if (Regex("[^\\w ^=]").containsMatchIn(input)) {
            try {
                //Выполнение расчета если ввод содержит любые знаки кроме =
                calcExpression()
            } catch (e: Exception) {
                println("Invalid expression")
            }
        } else {
            // Инициализация переменных и запись из память
            initVars()
        }
    }
}

private fun calcExpression() {
    //Подготовка ввода для преобразование в выражение PostFix
    parseInput()
    //Перевод выражения в PostFix
    getPostFixEx()
    //Расчет PostFix выражения
    calcResult()
}

private fun initLoop(scanner: Scanner) {
    input = scanner.nextLine()
    inputsList = input.split(" ") as MutableList<String>
    stack.clear()
    queue.clear()
    expressionList.clear()
}

private fun calcResult() {
    val stack = mutableListOf<BigInteger>()
    for (item in queue) {
        when {
            Regex("[\\da-zA-Z]").containsMatchIn(item) -> {
                if (variablesMap.containsKey(item)) {
                    stack.add(variablesMap.getValue(item))
                } else stack.add(item.toBigInteger())
            }
            item == "+" -> {
                stack[stack.lastIndex - 1] = stack[stack.lastIndex - 1] + stack.last()
                stack.removeAt(stack.lastIndex)
            }
            item == "*" -> {
                stack[stack.lastIndex - 1] = stack[stack.lastIndex - 1] * stack.last()
                stack.removeAt(stack.lastIndex)
            }
            item == "/" -> {
                stack[stack.lastIndex - 1] = stack[stack.lastIndex - 1] / stack.last()
                stack.removeAt(stack.lastIndex)
            }
            item == "-" -> {
                stack[stack.lastIndex - 1] = stack[stack.lastIndex - 1] - stack.last()
                stack.removeAt(stack.lastIndex)
            }
            item == "^" -> {
                stack[stack.lastIndex - 1] = stack[stack.lastIndex - 1].pow(stack.last().toInt())
                stack.removeAt(stack.lastIndex)
            }
        }
    }
    println(stack.first())
}

private fun parseInput() {
    /* Функция преобразует выражение в необходимое для преобразование
    в PostFix выражение, убирате двойные знаки +, пробелы и проверяет
     если пользователь ввел -- то это значит + */
    val sb = StringBuilder()
    var count = 0
    loop@ while (count <= input.length - 1) {
        when {
            input[count] == ' ' -> {
                // Пропускает все пробелы и выходит из данной итерации
                count++
                continue@loop
            }
            input[count].isDigit() || Regex("[a-zA-Z]").matches(input[count].toString()) -> {
                /* Проверка если обнаружена цифра и буква, далее идет проверка если цифра или буква не одна
                * то добавляем в билдер цифры или буквы и преобразовываем в число или в название
                * переменной */
                while (count <= input.length - 1 && (input[count].isDigit() ||
                            Regex("[a-zA-Z]").matches(input[count].toString()))
                ) {
                    sb.append(input[count])
                    count++
                }
                expressionList.add(sb.toString())
                sb.clear()
                continue@loop
            }
            Regex("[*/()]").matches(input[count].toString()) -> {
                expressionList.add(input[count].toString())
            }
            input[count] == '+' -> {
                while (count <= input.length - 1 &&
                    input[count] == '+'
                ) {
                    sb.append(input[count])
                    count++
                }
                expressionList.add(sb.first().toString())
                sb.clear()
                continue@loop
            }
            input[count] == '-' -> {
                while (count <= input.length - 1 &&
                    input[count] == '-'
                ) {
                    sb.append(input[count])
                    count++
                }
                if (sb.length == 2) {
                    expressionList.add("+")
                } else expressionList.add("-")
                sb.clear()
                continue@loop
            }
            input[count] == '^' -> {
                expressionList.add("^")
            }
        }
        count++
    }
}

private fun getPostFixEx() {
    expressionList.forEach {
        when {
            //Если ^ то просто добавляем в стэк
            it == "^" -> pushStack(it)

            //Если ( то просто добавляем в стэк
            it == "(" -> pushStack(it)

            /* Если ) то проверяем если в стэке нет (, то выбрасываем ошибку
            * если ) есть, то выгружаем стэк в очередь */
            it == ")" -> {
                if (expressionList.contains("(")) {
                    popStack()
                } else throw Exception("Error")
            }

            //Если цифра или буква то просто добавляем в очередь
            Regex("[\\da-zA-Z]").containsMatchIn(it) -> pushQueue(it)

            /* Если знак + или - , то проверяем: если стэк пустой или в вершина стэка "(",
            * то добавляем в стэк. Если же нет то провереям: Если вершина стэка боллее приоритетные операторы
             /*^*/ то выгружаем стэк и после добавляем в стэк данный оператор, если вершина стэка
             * содержит + или  - то выгружаем вершину в очередь а данный оператор устанвливаем на его место*/
            Regex("[+-]").containsMatchIn(it) ->
                if (stack.isEmpty() || stack.last() == "(") pushStack(it)
                else if (stack.last().contains(Regex("[/*^]"))) {
                    popStack()
                    pushStack(it)
                } else {
                    pushQueue(stack.last())
                    stack[stack.lastIndex] = it
                }

            /* Если оператор * или / то проверяем, если вершина стэка аналогичные,
            * то выгружаем стэк если ниже по приаритету то добавляем в стэк */
            Regex("[*/]").containsMatchIn(it) -> {
                if (stack.isNotEmpty() && (stack.last() == "*" || stack.last() == "/")) {
                    popStack()
                }
                pushStack(it)
            }
        }
    }

    /* Обработка последнего действия. Провека если стэк не пустой то добаляем все из стэка кроме (
    * в очередь */
    if (stack.isNotEmpty()) {
        for (i in stack.lastIndex downTo 0) {
            if (stack[i] != "(") {
                pushQueue(stack[i])
            } else throw Exception("Error")
        }
    }
}

private fun popStack() {
    Loop@ for (i in stack.lastIndex downTo 0) {
        if (stack[i] == "(") {
            stack[i] = " "
            break@Loop
        }
        pushQueue(stack[i])
        stack[i] = " "
    }
    stack.removeIf { it == " " }
}

private fun pushQueue(item: String) {
    queue.add(item)
}

private fun pushStack(item: String) {
    stack.add(item)
}

private fun initVars() {
    val tempList = input.trim().split("=") as MutableList<String>
    if (tempList.size == 1) {
        if (variablesMap.containsKey(tempList[0])) println(variablesMap.getValue(tempList[0]))
        else println("Unknown variable")
        return
    }
    tempList.removeIf { it == "" }
    addVarsMap(tempList)
}

private fun addVarsMap(tempList: MutableList<String>) {
    if (tempList.size == 2) {
        if (Regex("\\d").containsMatchIn(tempList[0])) {
            println("Invalid identifier")
            return
        }
        try {
            if (variablesMap.containsKey(tempList[1].trim())) {
                variablesMap[tempList[0].trim()] = variablesMap.getValue(tempList[1].trim())
            } else {
                variablesMap[tempList[0].trim()] = tempList[1].trim().toBigInteger()
            }
        } catch (e: Exception) {
            println("Invalid assignment")
            return
        }
    } else {
        println("Invalid assignment")
        return
    }
}
