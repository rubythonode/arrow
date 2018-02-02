---
layout: docs
title: SetKW
permalink: /docs/datatypes/setkw/
---

## SetKW

SetKW(Kinded Wrapper) is a higher kinded wrapper around the the Set collection interface. 

It can be created from the Kotlin Set type with a convient `k()` function.

```kotlin
import arrow.*
import arrow.core.*
import arrow.data.*

setOf(1, 2, 5, 3, 2).k()
//SetKW(set=[1, 2, 5, 3])
```

It can also be initialized with the following:

```kotlin
SetKW(setOf(1, 2, 5, 3, 2))
//SetKW(set=[1, 2, 5, 3])
```
or
```kotlin
SetKW.pure(1)
//SetKW(set=[1])
```

given the following:
```kotlin:ank
val oldNumbers = setOf( -11, 1, 3, 5, 7, 9).k()
val evenNumbers = setOf(-2, 4, 6, 8, 10).k()
val integers = setOf(-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5).k()
```
SetKW derives the following typeclasses:

[`Semigroup`](/docs/typeclasses/semigroupk/):
```kotlin
val uniqueNaturalNumbers_1 = oldNumbers.combineK(evenNumbers.combineK(integers))
uniqueNaturalNumbers_1
//SetKW(set=[-11, 1, 3, 5, 7, 9, -2, 4, 6, 8, 10, -5, -4, -3, -1, 0, 2])
```
```kotlin
val uniqueNaturalNumbers_2 = oldNumbers.combineK(evenNumbers).combineK(integers)
uniqueNaturalNumbers_2
//SetKW(set=[-11, 1, 3, 5, 7, 9, -2, 4, 6, 8, 10, -5, -4, -3, -1, 0, 2])
```
[`Foldable`](/docs/typeclasses/foldable/):
```kotlin
val sum_1 = uniqueNaturalNumbers_1.foldLeft(0){sum, number -> sum + (number * number)}
val sum_2 = uniqueNaturalNumbers_2.foldLeft(0){sum, number -> sum + (number * number)}
sum_1 == sum_2
//true
```
[`Monoid`](/docs/typeclasses/monoidk/):
```kotlin
val sum_3 = SetKW.monoid<Int>().combine(uniqueNaturalNumbers_1, SetKW.empty()).foldLeft(0){sum, number -> sum + (number * number)}
val sum_4 = SetKW.monoid<Int>().combine(SetKW.empty(), uniqueNaturalNumbers_1).foldLeft(0){sum, number -> sum + (number * number)}
sum_3 == sum_4
//true
```