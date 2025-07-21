[![ru](https://img.shields.io/badge/lang-ru-red.svg)](https://github.com/alex0x08/teleporta/blob/main/README.ru.md)

# Tiny XMLRPC
Моя супер миниатюрная реализация XML-RPC протокола, меньше 500 строк кода на Java, включая клиент и сервер.
[Клиент](https://github.com/alex0x08/tiny-xmlrpc/blob/main/library/src/main/java/com/Ox08/xmlrpc/XmlRpcClient.java) and 
[Сервер](https://github.com/alex0x08/tiny-xmlrpc/blob/main/library/src/main/java/com/Ox08/xmlrpc/XmlRpcServer.java).

[Тут](https://github.com/alex0x08/tiny-xmlrpc/tree/main/samples) находятся примеры использования этой библиотеки. [Клиент](https://github.com/alex0x08/tiny-xmlrpc/tree/main/samples/client), 
[сервер](https://github.com/alex0x08/tiny-xmlrpc/tree/main/samples/server) и [сервлет](https://github.com/alex0x08/tiny-xmlrpc/tree/main/samples/servlet)

# Статьи
Эта библиотека была создана для моей [статьи](https://blog.0x08.ru/tiny-xmlrpc-call-them-all), которая позже [была опубликована на Хабре](https://habr.com/ru/articles/837942/).


# В действии
Ниже скриншот с демонстрацией клиента и сервера, запущенных локально на одной машине:
![In action](https://github.com/alex0x08/tiny-xmlrpc/blob/main/images/in-action.jpg?raw=true)


# Примеры на других языках
Для доказательства правильности реализации, я сделал также несколько примеров использования на других языках:  [Tcl](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/test.tcl),
[Common Lisp](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/test-clisp-cbcl), [C++](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/test-xml-rpc.cpp),
[Python](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/test.py),[Perl](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/xml-rpc.pl).

Пример клиента на чистом C вызывающего мой XML-RPC сервер:

![In C](https://github.com/alex0x08/tiny-xmlrpc/blob/main/images/c-sample.png?raw=true)

Пример клиента на Haskell:

![Haskell](https://github.com/alex0x08/tiny-xmlrpc/blob/main/images/haskell-sample.png?raw=true)

FreePascal/Lazarus:

![Lazarus](https://github.com/alex0x08/tiny-xmlrpc/blob/main/images/lazarus-sample.png?raw=true)

# Сборка
Проект не имеет внешних зависимостей и может быть собран с любым JDK версии 17 и выше, с использованием обычного Apache Maven.
Достаточно выполнить из корневого каталога проекта:

```
mvn clean package
```
Запустится сборка библиотеки и всех примеров.
