package arrow.typeclasses

import arrow.HK
import arrow.TC
import arrow.core.Either
import arrow.core.Eval
import arrow.typeclass
import kotlin.coroutines.experimental.startCoroutine

@typeclass
interface Monad<F> : Applicative<F>, TC {

    fun <A, B> flatMap(fa: HK<F, A>, f: (A) -> HK<F, B>): HK<F, B>

    override fun <A, B> ap(fa: HK<F, A>, ff: HK<F, (A) -> B>): HK<F, B> = flatMap(ff, { f -> map(fa, f) })

    fun <A> flatten(ffa: HK<F, HK<F, A>>): HK<F, A> = flatMap(ffa, { it })

    fun <A, B> tailRecM(a: A, f: (A) -> HK<F, Either<A, B>>): HK<F, B>

    fun <A, B> followedBy(fa: HK<F, A>, fb: HK<F, B>): HK<F, B> = flatMap(fa, { fb })

    fun <A, B> followedByEval(fa: HK<F, A>, fb: Eval<HK<F, B>>): HK<F, B> = flatMap(fa, { fb.value() })

    fun <A, B> forEffect(fa: HK<F, A>, fb: HK<F, B>): HK<F, A> = flatMap(fa, { a -> map(fb, { a }) })

    fun <A, B> forEffectEval(fa: HK<F, A>, fb: Eval<HK<F, B>>): HK<F, A> = flatMap(fa, { a -> map(fb.value(), { a }) })
}

/**
 * Entry point for monad bindings which enables for comprehension. The underlying implementation is based on coroutines.
 * A coroutine is initiated and suspended inside [MonadErrorContinuation] yielding to [Monad.flatMap]. Once all the flatMap binds are completed
 * the underlying monad is returned from the act of executing the coroutine
 */
fun <F, B> Monad<F>.binding(c: suspend MonadContinuation<F, *>.() -> B): HK<F, B> {
    val continuation = MonadContinuation<F, B>(this)
    val wrapReturn: suspend MonadContinuation<F, *>.() -> HK<F, B> = { pure(c()) }
    wrapReturn.startCoroutine(continuation, continuation)
    return continuation.returnedMonad()
}
