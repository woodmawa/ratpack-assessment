package com.softwood

import ratpack.exec.Blocking
import ratpack.exec.Downstream
import ratpack.exec.ExecResult
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification


//see https://danhyun.github.io/mastering-async-ratpack/


class RatpackAsyncTest extends Specification {

    /*
     * Auto cleanup does spock tidy up
     * delegate calls to execHarness e.g. yeild ()
     */
    @AutoCleanup
    //@Shared
    @Delegate
    ExecHarness execHarness = ExecHarness.harness()

    /*def cleanupSpec() {
        execHarness.close()

    }*/

    //not a test, but used by one
    def someExternal (Closure callback) {
        //do work
        //when done call callback
        callback ()
    }

    def "simple async test" () {

        expect:
        true
    }

    def "ratpack promise value " () {

        given :
        ExecHarness harness = ExecHarness.harness()
        Promise promise = Promise.value('hello')
        def result = harness.yield {promise}

        expect:
        result.value == 'hello'

        cleanup:
        harness.close()
    }

    def "ratpack promise sync " () {

        given :
        Promise promise = Promise.sync {
            println "eval sync"
            "sync"
        }
        def result = yield {promise}

        expect:
        result.value == 'sync'

    }

    def "ratpack promise async " () {
        given :
        Promise promise = Promise.async() { Downstream ds ->
            println "async start"
            Closure callback = {
                ds.success("async computation complete")
            }

            //call external work, and pass callback to be called when complete
            someExternal (callback)
            println "async end "
        }
        def result = yield {promise}

        expect:
        result.value == 'async computation complete'
    }

    def "ratpack promise blocking " () {
        given :
        Promise promise = Blocking.get {
            println "eval in blocking "
            "from blocking"
        }
        def result = yield {promise}

        expect:
        result.value == 'from blocking'
    }

    def "ratpack on mixed threadpools" () {
        given:
        Closure getCurrentThreadName = {
            return Thread.currentThread().name.split('-')[1]
        }

        and:
        Promise p = Blocking.get {
            Thread.sleep(1000)
            getCurrentThreadName()
        } map { String threadNameFromBlocking ->
            String name = getCurrentThreadName()
            return [threadNameFromBlocking, name].join(' -> ')
        }

        when:
        ExecResult result = yield { p }

        then:
        result.value == 'blocking -> compute'
    }

    def "ratpack chain a promise " () {
        given :
        Promise promise = Promise.sync {
            return "hello"
        }.map {it.toUpperCase()}

        ExecResult execResult = yield {promise
            def result = promise.wiretap {it.value}
            promise.then {println " and then ... $it"} //void return
            result
        }
        //promise.then {println " and then ... $it"}

        expect:
        execResult.value == 'HELLO'

    }
}
