# Retrofit 笔记

**一开始的目的是为了学习动态代理的使用，后来干脆把整个流程梳理了下**

这不是完整的`Retrofit`，是纯`Java`环境下不涉及`Http`的流程代码，`Retrofit`的精简代码，把内部一些设计和逻辑做了精简，
主要是为了学习整个代码的大体设计和流程

`Retrofit`内关于`OkHttp、Rx、Android`相关的部分都去除了，其中`okhttp3.call`由一个`RawCall`来代替，
内部没有做相关网络请求，只是做了下字符串拼接后`println()`

**完整代码**——>[GitHub RetrofitDemo](https://github.com/PoetGcc/RetrofitDemo/tree/master)

***

