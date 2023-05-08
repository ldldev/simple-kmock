package dev.ldldevelopers.simplekmock

typealias Predicate<T> = (T) -> Boolean

/** Accepts anything */
public fun <T> any(): Predicate<T> = { true }
/** Accepts only values equal to t */
public fun <T> equalTo(t: T): Predicate<T> = { it == t }
/** Accepts only values distinct from t */
public fun <T> notEqualTo(t: T): Predicate<T> = equalTo(t).not()
/** Accepts only null values */
public fun <T> nullValue(): Predicate<T?> = equalTo(null)
/** Accepts only non-null values */
public fun <T> notNullValue(): Predicate<T?> = nullValue<T?>().not()
/** Accepts only values strictly grater than t */
public fun <T : Comparable<T>> greaterThan(t: T): Predicate<T> = { it > t }
/** Accepts only values greater than or equal to t*/
public fun <T : Comparable<T>> greaterThanOrEqualTo(t: T): Predicate<T> = { it >= t }
/** Accepts only values less than t */
public fun <T : Comparable<T>> lessThan(t: T): Predicate<T> = { it < t }
/** Accepts only values less than or equal to t */
public fun <T : Comparable<T>> lessThanOrEqualTo(t: T): Predicate<T> = { it <= t }
/** Accepts only collections containing t */
public fun <T> containing(t: T): Predicate<Collection<T>> = { it.contains(t) }
/** Accepts only collections not containing t */
public fun <T> notContaining(t: T): Predicate<Collection<T>> = containing(t).not()
/** Accepts only character sequences starting with prefix */
public fun startingWith(prefix: CharSequence): Predicate<CharSequence> = { it.startsWith(prefix) }
/** Accepts only character sequences not starting with prefix */
public fun notStartingWith(prefix: CharSequence): Predicate<CharSequence> = startingWith(prefix).not()
/** Accepts only character sequences starting with prefix ignoring case */
public fun startingWithIgnoringCase(prefix: CharSequence): Predicate<CharSequence> = { it.startsWith(prefix, true) }
/** Accepts only character sequences not starting with prefix ignoring case */
public fun notStartingWithIgnoringCase(prefix: CharSequence): Predicate<CharSequence> = startingWithIgnoringCase(prefix).not()
/** Accepts only character sequences ending with suffix */
public fun endingWith(suffix: CharSequence): Predicate<CharSequence> = { it.endsWith(suffix) }
/** Accepts only character sequences not ending with suffix */
public fun notEndingWith(suffix: CharSequence): Predicate<CharSequence> = endingWith(suffix).not()
/** Accepts only character sequences ending with suffix ignoring case */
public fun endingWithIgnoringCase(suffix: CharSequence): Predicate<CharSequence> = { it.endsWith(suffix, true) }
/** Accepts only character sequences not ending with suffix ignoring case */
public fun notEndingWithIgnoringCase(suffix: CharSequence): Predicate<CharSequence> = endingWithIgnoringCase(suffix).not()

/** Joins this predicate to another with a non-short-circuit "and" */
public fun <T> Predicate<T>.and(other: Predicate<T>): Predicate<T> = { this(it) and other(it) }
/** Joins this predicate to another with a short-circuit "and" */
public operator fun <T> Predicate<T>.times(other: Predicate<T>): Predicate<T> = { this(it) && other(it) }
/** Joins this predicate to another with a non-short-circuit "or" */
public fun <T> Predicate<T>.or(other: Predicate<T>): Predicate<T> = { this(it) or other(it) }
/** Joins this predicate to another with a non-short-circuit "or" */
public operator fun <T> Predicate<T>.plus(other: Predicate<T>): Predicate<T> = { this(it) || other(it) }
/** Joins this predicate to another with a "xor" */
public fun <T> Predicate<T>.xor(other: Predicate<T>): Predicate<T> = { this(it) xor other(it) }
/** Negates this predicate */
public operator fun <T> Predicate<T>.not(): Predicate<T> = { !this(it) }
/** Creates a predicate for nullable types by accepting only non-null values that satisfy this predicate */
public fun <T> Predicate<T>.andNotNull(): Predicate<T?> = { it != null && this(it) }
/** Creates a predicate for nullable types by accepting all null values and those that satisfy this predicate*/
public fun <T> Predicate<T>.orNull(): Predicate<T?> = { it == null || this(it) }
/** Accepts only collections where all elements satisfy this predicate */
public fun <T> Predicate<T>.forAll(): Predicate<Collection<T>> = { it.all(this) }
/** Accepts only collections where at least one element doesn't satisfy this predicate */
public fun <T> Predicate<T>.forNotAll(): Predicate<Collection<T>> = forAll().not()
/** Accepts only collections where at least one element satisfies this predicate */
public fun <T> Predicate<T>.forSome(): Predicate<Collection<T>> = { it.any(this) }
/** Accepts only collections where at least one element satisfies this predicate */
public fun <T> Predicate<T>.forAny(): Predicate<Collection<T>> = forSome()
/** Accepts only collections where no elements satisfy this predicate */
public fun <T> Predicate<T>.forNone(): Predicate<Collection<T>> = forSome().not()
/** Accepts only collections where only one element satisfies this predicate */
public fun <T> Predicate<T>.forOne(): Predicate<Collection<T>> = { it.count(this) == 1 }
/** Accepts only collections where exactly n elements satisfy this predicate */
public fun <T> Predicate<T>.forN(n: Int): Predicate<Collection<T>> = { it.count(this) == n }
/** Accepts only collections where at least (non-strict) n elements satisfy this predicate */
public fun <T> Predicate<T>.forAtLeastN(n: Int): Predicate<Collection<T>> = { it.count(this) >= n }
/** Accepts only collections where at most (non-strict) n elements satisfy this predicate */
public fun <T> Predicate<T>.forAtMostN(n: Int): Predicate<Collection<T>> = { it.count(this) <= n }

/** Allows writing a predicate with a receiver */
public fun <T> predicateWithReceiver(predicate: T.() -> Boolean): Predicate<T> = predicate
