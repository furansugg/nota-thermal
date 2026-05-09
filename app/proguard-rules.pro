# Keep ESC/POS printer library classes used via reflection / JNI-ish
-keep class com.dantsu.escposprinter.** { *; }

# Room: keep generated impls
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
