package dev.ldldevelopers.simplekmock

typealias Predicate<T> = (T) -> Boolean

public fun <T> any(): Predicate<T> = { true }
public fun <T> equalTo(t: T): Predicate<T> = { it == t }
public fun <T> notEqualTo(t: T): Predicate<T> = equalTo(t).not()
public fun <T> nullValue(): Predicate<T?> = equalTo(null)
public fun <T> notNullValue(): Predicate<T?> = nullValue<T?>().not()
public fun <T : Comparable<T>> greaterThan(t: T): Predicate<T> = { it > t }
public fun <T : Comparable<T>> greaterThanOrEqualTo(t: T): Predicate<T> = { it >= t }
public fun <T : Comparable<T>> lessThan(t: T): Predicate<T> = { it < t }
public fun <T : Comparable<T>> lessThanOrEqualTo(t: T): Predicate<T> = { it <= t }
public fun <T> containing(t: T): Predicate<Collection<T>> = { it.contains(t) }
public fun <T> notContaining(t: T): Predicate<Collection<T>> = containing(t).not()
public fun startingWith(prefix: CharSequence): Predicate<CharSequence> = { it.startsWith(prefix) }
public fun notStartingWith(prefix: CharSequence): Predicate<CharSequence> = startingWith(prefix).not()
public fun startingWithIgnoringCase(prefix: CharSequence): Predicate<CharSequence> = { it.startsWith(prefix, true) }
public fun notStartingWithIgnoringCase(prefix: CharSequence): Predicate<CharSequence> = startingWithIgnoringCase(prefix).not()
public fun endingWith(suffix: CharSequence): Predicate<CharSequence> = { it.endsWith(suffix) }
public fun notEndingWith(suffix: CharSequence): Predicate<CharSequence> = endingWith(suffix).not()
public fun endingWithIgnoringCase(suffix: CharSequence): Predicate<CharSequence> = { it.endsWith(suffix, true) }
public fun notEndingWithIgnoringCase(suffix: CharSequence): Predicate<CharSequence> = endingWithIgnoringCase(suffix).not()

public fun <T> Predicate<T>.and(other: Predicate<T>): Predicate<T> = { this(it) and other(it) }
public operator fun <T> Predicate<T>.times(other: Predicate<T>): Predicate<T> = { this(it) && other(it) }
public fun <T> Predicate<T>.or(other: Predicate<T>): Predicate<T> = { this(it) or other(it) }
public operator fun <T> Predicate<T>.plus(other: Predicate<T>): Predicate<T> = { this(it) || other(it) }
public fun <T> Predicate<T>.xor(other: Predicate<T>): Predicate<T> = { this(it) xor other(it) }
public operator fun <T> Predicate<T>.not(): Predicate<T> = { !this(it) }
public fun <T> Predicate<T>.andNotNull(): Predicate<T?> = { it != null && this(it) }
public fun <T> Predicate<T>.orNull(): Predicate<T?> = { it == null || this(it) }
public fun <T> Predicate<T>.forAll(): Predicate<Collection<T>> = { it.all(this) }
public fun <T> Predicate<T>.forNotAll(): Predicate<Collection<T>> = forAll().not()
public fun <T> Predicate<T>.forSome(): Predicate<Collection<T>> = { it.any(this) }
public fun <T> Predicate<T>.forAny(): Predicate<Collection<T>> = forSome()
public fun <T> Predicate<T>.forNone(): Predicate<Collection<T>> = forSome().not()
public fun <T> Predicate<T>.forOne(): Predicate<Collection<T>> = { it.count(this) == 1 }
public fun <T> Predicate<T>.forN(n: Int): Predicate<Collection<T>> = { it.count(this) == n }
public fun <T> Predicate<T>.forAtLeastN(n: Int): Predicate<Collection<T>> = { it.count(this) >= n }
public fun <T> Predicate<T>.forAtMostN(n: Int): Predicate<Collection<T>> = { it.count(this) <= n }
public fun <T> predicateWithReceiver(predicate: T.() -> Boolean): Predicate<T> = predicate
