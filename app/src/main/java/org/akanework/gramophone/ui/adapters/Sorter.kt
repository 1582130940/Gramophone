/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.ui.adapters

import android.net.Uri
import org.akanework.gramophone.logic.comparators.SupportComparator
import org.akanework.gramophone.logic.utils.CalculationUtils
import java.io.File

class Sorter<T>(
    val sortingHelper: Helper<T>,
    private val naturalOrderHelper: NaturalOrderHelper<T>?,
    private val rawOrderExposed: Boolean = false
) {

    abstract class Helper<T>(typesSupported: Set<Type>) {
        init {
            if (typesSupported.contains(Type.NaturalOrder) || typesSupported.contains(Type.None))
                throw IllegalStateException()
        }

        val typesSupported = typesSupported.toMutableSet().apply { add(Type.None) }.toSet()
        abstract fun getTitle(item: T): String?
        abstract fun getId(item: T): String
        abstract fun getCover(item: T): Uri?

        open fun getArtist(item: T): String? = throw UnsupportedOperationException()
        open fun getFile(item: T): File = throw UnsupportedOperationException()
        open fun getAlbumTitle(item: T): String? = throw UnsupportedOperationException()
        open fun getAlbumArtist(item: T): String? = throw UnsupportedOperationException()
        open fun getSize(item: T): Int = throw UnsupportedOperationException()
        open fun getAlbumSize(item: T): Int = throw UnsupportedOperationException()
        open fun getAddDate(item: T): Long = throw UnsupportedOperationException()
        open fun getReleaseDate(item: T): Long = throw UnsupportedOperationException()
        open fun getModifiedDate(item: T): Long = throw UnsupportedOperationException()
        open fun getDiscAndTrack(item: T): Int = throw UnsupportedOperationException()
        fun canGetTitle(): Boolean = typesSupported.contains(Type.ByTitleAscending)
                || typesSupported.contains(Type.ByTitleDescending)

        fun canGetArtist(): Boolean = typesSupported.contains(Type.ByArtistAscending)
                || typesSupported.contains(Type.ByArtistDescending)

        fun canGetAlbumTitle(): Boolean = typesSupported.contains(Type.ByAlbumTitleAscending)
                || typesSupported.contains(Type.ByAlbumTitleDescending)

        fun canGetAlbumArtist(): Boolean = typesSupported.contains(Type.ByAlbumArtistAscending)
                || typesSupported.contains(Type.ByAlbumArtistDescending)

        fun canGetSize(): Boolean = typesSupported.contains(Type.BySizeAscending)
                || typesSupported.contains(Type.BySizeDescending)

        fun canGetAlbumSize(): Boolean = typesSupported.contains(Type.ByAlbumSizeAscending)
                || typesSupported.contains(Type.ByAlbumSizeDescending)

        fun canGetDiskAndTrack(): Boolean = typesSupported.contains(Type.ByDiscAndTrack)

        fun canGetAddDate(): Boolean = typesSupported.contains(Type.ByAddDateAscending)
                || typesSupported.contains(Type.ByAddDateDescending)

        fun canGetFile(): Boolean = typesSupported.contains(Type.ByFilePathAscending)
                || typesSupported.contains(Type.ByFilePathDescending)

        fun canGetReleaseDate(): Boolean = typesSupported.contains(Type.ByReleaseDateAscending)
                || typesSupported.contains(Type.ByReleaseDateDescending)

        fun canGetModifiedDate(): Boolean = typesSupported.contains(Type.ByModifiedDateAscending)
                || typesSupported.contains(Type.ByModifiedDateDescending)
    }

    fun interface NaturalOrderHelper<T> {
        fun lookup(item: T): Int
    }

    enum class Type {
        ByTitleDescending, ByTitleAscending,
        ByArtistDescending, ByArtistAscending,
        ByAlbumTitleDescending, ByAlbumTitleAscending,
        ByAlbumArtistDescending, ByAlbumArtistAscending,
        BySizeDescending, BySizeAscending,
        ByAlbumSizeDescending, ByAlbumSizeAscending,
        NaturalOrder, ByAddDateDescending, ByAddDateAscending,
        ByReleaseDateDescending, ByReleaseDateAscending,
        ByModifiedDateDescending, ByModifiedDateAscending,
        ByFilePathDescending, ByFilePathAscending,
        ByDiscAndTrack,

        /* do not use NativeOrder for something other than title or edit getSupportedTypes */
        None, NativeOrder, NativeOrderDescending
    }

    fun getSupportedTypes(): Set<Type> {
        return sortingHelper.typesSupported.let { types ->
            (if (naturalOrderHelper != null)
                types + Type.NaturalOrder
            else types).let {
                if (rawOrderExposed)
                    it + Type.NativeOrder + Type.NativeOrderDescending -
                            Type.ByTitleAscending - Type.ByTitleDescending
                else it
            }
        }
    }

    fun getComparator(type: Type): HintedComparator<T>? {
        if (!getSupportedTypes().contains(type))
            throw IllegalArgumentException("Unsupported type ${type.name}")
        if (type == Type.NativeOrder || type == Type.NativeOrderDescending) return null
        return WrappingHintedComparator(type, when (type) {
            Type.ByTitleDescending -> {
                SupportComparator.createAlphanumericComparator(true, {
                    sortingHelper.getTitle(it) ?: ""
                }, null)
            }

            Type.ByTitleAscending -> {
                SupportComparator.createAlphanumericComparator(false, {
                    sortingHelper.getTitle(it) ?: ""
                }, null)
            }

            Type.ByArtistDescending -> {
                SupportComparator.createAlphanumericComparator(
                    true, {
                        sortingHelper.getArtist(it) ?: ""
                    }, getComparator(
                        if (rawOrderExposed)
                            Type.NativeOrderDescending else Type.ByTitleDescending
                    )
                )
            }

            Type.ByArtistAscending -> {
                SupportComparator.createAlphanumericComparator(
                    false,
                    {
                        sortingHelper.getArtist(it) ?: ""
                    },
                    getComparator(if (rawOrderExposed) Type.NativeOrder else Type.ByTitleAscending)
                )
            }

            Type.ByAlbumTitleDescending -> {
                SupportComparator.createAlphanumericComparator(true, {
                    sortingHelper.getAlbumTitle(it) ?: ""
                }, getComparator(Type.ByDiscAndTrack))
            }

            Type.ByAlbumTitleAscending -> {
                SupportComparator.createAlphanumericComparator(false, {
                    sortingHelper.getAlbumTitle(it) ?: ""
                }, getComparator(Type.ByDiscAndTrack))
            }

            // TODO support choosing album artist > album year > disc/track
            Type.ByAlbumArtistDescending -> {
                SupportComparator.createAlphanumericComparator(true, {
                    sortingHelper.getAlbumArtist(it) ?: ""
                }, getComparator(Type.ByAlbumTitleDescending))
            }

            Type.ByAlbumArtistAscending -> {
                SupportComparator.createAlphanumericComparator(false, {
                    sortingHelper.getAlbumArtist(it) ?: ""
                }, getComparator(Type.ByAlbumTitleAscending))
            }

            Type.BySizeDescending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getSize(it) }, true
                )
            }

            Type.BySizeAscending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getSize(it) }, false
                )
            }

            Type.ByAlbumSizeDescending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getAlbumSize(it) }, true
                )
            }

            Type.ByAlbumSizeAscending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getAlbumSize(it) }, false
                )
            }

            Type.ByAddDateDescending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getAddDate(it) }, true
                )
            }

            Type.ByAddDateAscending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getAddDate(it) }, false
                )
            }

            Type.ByReleaseDateDescending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getReleaseDate(it) }, true,
                    if (sortingHelper.canGetDiskAndTrack()) getComparator(Type.ByDiscAndTrack) else null
                )
            }

            Type.ByReleaseDateAscending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getReleaseDate(it) }, false,
                    if (sortingHelper.canGetDiskAndTrack()) getComparator(Type.ByDiscAndTrack) else null
                )
            }

            Type.ByModifiedDateDescending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getModifiedDate(it) }, true
                )
            }

            Type.ByModifiedDateAscending -> {
                SupportComparator.createInversionComparator(
                    compareBy { sortingHelper.getModifiedDate(it) }, false
                )
            }

            Type.ByFilePathDescending -> {
                SupportComparator.createAlphanumericComparator(true, {
                    sortingHelper.getFile(it).path
                }, null)
            }

            Type.ByFilePathAscending -> {
                SupportComparator.createAlphanumericComparator(false, {
                    sortingHelper.getFile(it).path
                }, null)
            }

            Type.ByDiscAndTrack -> {
                compareBy { sortingHelper.getDiscAndTrack(it) }
            }

            Type.NaturalOrder -> {
                SupportComparator.createInversionComparator(
                    compareBy { naturalOrderHelper!!.lookup(it) }, false
                )
            }

            Type.None -> SupportComparator.createDummyComparator()

            else -> throw IllegalStateException("this code is unreachable")
        }
        )
    }

    fun getFastScrollHintFor(item: T, sortType: Type): String? {
        return when (sortType) {
            Type.ByTitleDescending, Type.ByTitleAscending, Type.NativeOrder, Type.NativeOrderDescending -> {
                sortingHelper.getTitle(item)?.firstOrNull()?.toString()
            }

            Type.ByArtistDescending, Type.ByArtistAscending -> {
                sortingHelper.getArtist(item)?.firstOrNull()?.toString()
            }

            Type.ByAlbumTitleDescending, Type.ByAlbumTitleAscending -> {
                sortingHelper.getAlbumTitle(item)?.firstOrNull()?.toString()
            }

            Type.ByAlbumArtistDescending, Type.ByAlbumArtistAscending -> {
                sortingHelper.getAlbumArtist(item)?.firstOrNull()?.toString()
            }

            Type.ByFilePathDescending, Type.ByFilePathAscending -> {
                null // can probably not do anything better
            }

            Type.BySizeDescending, Type.BySizeAscending -> {
                sortingHelper.getSize(item).toString()
            }

            Type.ByAlbumSizeAscending, Type.ByAlbumSizeDescending -> {
                sortingHelper.getAlbumSize(item).toString()
            }

            Type.ByDiscAndTrack -> {
                sortingHelper.getDiscAndTrack(item).toString()
            }

            Type.ByAddDateDescending, Type.ByAddDateAscending -> {
                CalculationUtils.convertUnixTimestampToMonthDay(sortingHelper.getAddDate(item))
            }

            Type.ByReleaseDateDescending, Type.ByReleaseDateAscending -> {
                CalculationUtils.convertUnixTimestampToMonthDay(sortingHelper.getReleaseDate(item))
            }

            Type.ByModifiedDateDescending, Type.ByModifiedDateAscending -> {
                CalculationUtils.convertUnixTimestampToMonthDay(sortingHelper.getAddDate(item))
            }

            Type.NaturalOrder -> {
                naturalOrderHelper!!.lookup(item).toString()
            }

            Type.None -> null
        }?.ifEmpty { null }?.uppercase()
    }

    abstract class HintedComparator<T>(val type: Type) : Comparator<T>
    private class WrappingHintedComparator<T>(type: Type, private val comparator: Comparator<T>) :
        HintedComparator<T>(type) {
        override fun compare(o1: T, o2: T): Int {
            return comparator.compare(o1, o2)
        }
    }
}
