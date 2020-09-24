## 음악 재생 어플리케이션_Simple MusicPlayer
Music Player Application


### Description

>
    
    Project in Ljubljana University
    
    It is an music player that includes simple player function, like pause, play, stop, exit and special function which uses Motion recognization.
    
    
   
### Details

+ The commands Play, Pause, Stop and Exit are available both through the MainActivity
and through foreground notifications

+ Command GesturesOn/GesturesOff

  If User click GesturesOn button, then user can Gesture-based commands.
  
    + Case1. User shakes the phone horizontally(left-right) : Music Pauses
    + Case2. User shakes the phone vertically(up-down) : Music plays
    
+ Architecture

  Communication : MainActivity​<==>​MediaPlayerService​<==>A​ ccelerationService​<=​=​>​SensorManager​(Android)
