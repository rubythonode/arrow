package arrow.weak

import arrow.core.*
import arrow.higherkind
import arrow.weak.internal.WeakRef

/**
 * Represents an object that **can** stop existing (recycled by the Garbage Collector) when is no longer referenced.
 * Backed by a [WeakReference] instance. In a similar fashion to [Option] this forces the consumer to check whether the
 * object is still valid.
 *
 * We have several ways to create a new [Weak] instance:
 *
 *  - `Weak(object)`: Creates an instance with a weak reference to the provided object
 *  - `object.weak()`: An alias for the previous method
 *  - `Weak.emptyWeak()`: Represents an instance that will never exist. Used for operations and tests.
 *
 *  At the time of usage we can either apply functional operators or unwrap it in two forms:
 *
 *   - `weakObject.eval`: [Eval] property that provides an [Option] with the result or lack thereof.
 *   - `weakObject.option()`: to get the [Option] directly.
 */
@higherkind
class Weak<out A> private constructor(val eval: Eval<Option<A>>) : WeakKind<A> {

    companion object {

        private val EMPTY: Weak<Nothing> = Weak(Eval.now(Option.empty()))

        @Suppress("UNCHECKED_CAST")
        fun <B> emptyWeak(): Weak<B> = EMPTY

        operator fun <A> invoke(a: A): Weak<A> {
            val reference = WeakRef(a)
            return Weak(Eval.always { Option.fromNullable(reference.get()) })// { reference.get() }
        }

        tailrec fun <A, B> tailRectM(a: A, f: (A) -> WeakKind<Either<A, B>>): Weak<B> {
            val option: Option<Either<A, B>> = f(a).ev().option()
            return when (option) {
                is None -> emptyWeak()
                is Some -> {
                    val either = option.t
                    when (either) {
                        is Either.Left -> tailRectM(either.a, f)
                        is Either.Right -> either.b.weak()
                    }
                }
            }
        }
    }

    fun option(): Option<A> = eval.value()

    inline fun <B> fold(fn: () -> B, f: (A) -> B): B = option().fold(fn, f)

    inline fun <B> map(crossinline f: (A) -> B): Weak<B> = fold({ emptyWeak() }, { f(it).weak() })

    inline fun <B> flatMap(crossinline f: (A) -> WeakKind<B>): Weak<B> = fold({ emptyWeak() }, { f(it).ev() })

    /**
     * Returns this Weak as long as the provided predicate confirms the value is to be kept
     *
     * @param p Predicate used for testing
     */
    inline fun filter(crossinline p: (A) -> Boolean): Weak<A> = fold({ emptyWeak() }, { a -> if (p(a)) a.weak() else emptyWeak() })

    fun <B> ap(ff: WeakKind<(A) -> B>): Weak<B> = ff.ev().flatMap { f -> map(f) }.ev()

    fun exists(predicate: Predicate<A>): Boolean = fold({ false }, { predicate(it) })

    fun <B> foldLeft(b: B, f: (B, A) -> B): B = ev().fold({ b }, { f(b, it) })

    fun <B> foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> = ev().fold({ lb }, { f(it, lb) })

    fun forall(predicate: Predicate<A>): Boolean = fold({ false }, { predicate(it) })

    fun isEmpty(): Boolean = fold({ true }, { false })

    fun nonEmpty(): Boolean = fold({ false }, { true })
}

/**
 * Returns the internal value or an alternative if we've lost it.
 *
 * @param fallback provides a new value if we have lost the current one.
 */
fun <B> WeakKind<B>.getOrElse(fallback: () -> B): B = ev().fold({ fallback() }, { it })

/**
 * Returns this Weak instance if present or an alternative.
 *
 * @param fallback provides a new value if we have lost the current one.
 */
fun <A, B : A> WeakKind<B>.orElse(fallback: () -> Weak<B>): Weak<B> = ev().fold({ fallback() }, { it.weak() })

/**
 * Creates a new Weak instance. Alias of [Weak.invoke].
 */
fun <A> A.weak(): Weak<A> = Weak(this)
