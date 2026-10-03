import React, { useState, useEffect, useRef } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, ActivityIndicator, StatusBar, SafeAreaView, Animated } from 'react-native';
import { Audio } from 'expo-av';

// Configuration Serverless GitOps
const GITHUB_RAW_URL = "https://raw.githubusercontent.com/larneb45/LAYA-Origin/main/playlist.json";

// Algorithme déterministe (Le Mix de Minuit) avec la date du jour en graine (seed)
function getSeedFromToday() {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return parseInt(`${y}${m}${d}`, 10);
}

function deterministicShuffle(array, seed) {
  const shuffled = [...array];
  let s = seed;
  const pseudoRandom = () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(pseudoRandom() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
}

export default function App() {
  const [playlist, setPlaylist] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);

  const soundRef = useRef(null);
  const blinkAnim = useRef(new Animated.Value(1)).current;

  // Animation Live clignotant
  useEffect(() => {
    if (isPlaying) {
      Animated.loop(
        Animated.sequence([
          Animated.timing(blinkAnim, { toValue: 0.2, duration: 800, useNativeDriver: true }),
          Animated.timing(blinkAnim, { toValue: 1, duration: 800, useNativeDriver: true })
        ])
      ).start();
    } else {
      blinkAnim.setValue(1);
    }
  }, [isPlaying]);

  // Initialisation audio background & fetch playlist
  useEffect(() => {
    async function setupAudio() {
      try {
        await Audio.setAudioModeAsync({
          staysActiveInBackground: true,
          playsInSilentModeIOS: true,
          shouldDuckAndroid: true,
          playThroughEarpieceAndroid: false
        });
      } catch (e) {
        console.warn('Audio mode error:', e);
      }
    }

    async function loadPlaylist() {
      setIsLoading(true);
      try {
        const response = await fetch(GITHUB_RAW_URL + '?t=' + Date.now());
        let data;
        if (response.ok) {
          data = await response.json();
        } else {
          // Fallback par défaut
          data = [
            { id: 1, title: "Midnight Reflections", artist: "LAYA Origin", url: "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3" },
            { id: 2, title: "Zen Garden Dawn", artist: "LAYA Origin", url: "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3" },
            { id: 3, title: "Solar Wind Echoes", artist: "LAYA Origin", url: "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3" }
          ];
        }

        // Application du Mix de Minuit
        const seed = getSeedFromToday();
        const midnightMix = deterministicShuffle(data, seed);
        setPlaylist(midnightMix);
        setCurrentIndex(0);
      } catch (err) {
        setErrorMsg("Erreur de connexion à GitHub");
      } finally {
        setIsLoading(false);
      }
    }

    setupAudio();
    loadPlaylist();

    return () => {
      if (soundRef.current) {
        soundRef.current.unloadAsync();
      }
    };
  }, []);

  async function playTrack(index) {
    if (!playlist || playlist.length === 0) return;
    try {
      setIsLoading(true);
      if (soundRef.current) {
        await soundRef.current.unloadAsync();
      }
      const track = playlist[index];
      const { sound } = await Audio.Sound.createAsync(
        { uri: track.url },
        { shouldPlay: true },
        onPlaybackStatusUpdate
      );
      soundRef.current = sound;
      setCurrentIndex(index);
      setIsPlaying(true);
    } catch (e) {
      console.warn("Erreur lecture:", e);
    } finally {
      setIsLoading(false);
    }
  }

  function onPlaybackStatusUpdate(status) {
    if (status.isLoaded) {
      setIsPlaying(status.isPlaying);
      if (status.didJustFinish) {
        nextTrack();
      }
    }
  }

  async function togglePlayPause() {
    if (!soundRef.current) {
      await playTrack(currentIndex);
      return;
    }
    if (isPlaying) {
      await soundRef.current.pauseAsync();
      setIsPlaying(false);
    } else {
      await soundRef.current.playAsync();
      setIsPlaying(true);
    }
  }

  async function nextTrack() {
    if (playlist.length === 0) return;
    const nextIdx = (currentIndex + 1) % playlist.length;
    await playTrack(nextIdx);
  }

  async function prevTrack() {
    if (playlist.length === 0) return;
    const prevIdx = (currentIndex - 1 + playlist.length) % playlist.length;
    await playTrack(prevIdx);
  }

  const currentTrack = playlist[currentIndex] || { title: "Chargement...", artist: "LAYA Origin" };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#FAF7F2" />
      <View style={styles.header}>
        <Text style={styles.brandTitle}>LAYA Origin</Text>
        <Animated.View style={[styles.liveBadge, { opacity: isPlaying ? blinkAnim : 0.4 }]}>
          <View style={styles.liveDot} />
          <Text style={styles.liveText}>LIVE</Text>
        </Animated.View>
      </View>

      <View style={styles.artworkContainer}>
        <View style={styles.artCircle}>
          <Text style={styles.artIcon}>♫</Text>
        </View>
        <Text style={styles.mixSubtitle}>LE MIX DE MINUIT</Text>
        <Text style={styles.trackTitle}>{currentTrack.title || currentTrack.titre}</Text>
        <Text style={styles.artistName}>{currentTrack.artist || 'Webradio Collective'}</Text>
      </View>

      <View style={styles.controlsRow}>
        <TouchableOpacity style={styles.navButton} onPress={prevTrack}>
          <Text style={styles.navButtonText}>◀◀</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.playButton} onPress={togglePlayPause} activeOpacity={0.85}>
          {isLoading ? (
            <ActivityIndicator color="#FAF7F2" size="large" />
          ) : (
            <Text style={styles.playButtonText}>{isPlaying ? "PAUSE" : "START"}</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity style={styles.navButton} onPress={nextTrack}>
          <Text style={styles.navButtonText}>▶▶</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.footer}>
        <Text style={styles.footerText}>
          Piste {currentIndex + 1} / {playlist.length || 0} • GitHub Serverless GitOps
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FAF7F2',
    justifyContent: 'space-between',
    paddingHorizontal: 24,
    paddingVertical: 16
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 10
  },
  brandTitle: {
    fontSize: 22,
    fontWeight: '700',
    color: '#394A41',
    letterSpacing: 0.5
  },
  liveBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFEBE8',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 14
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#E05345',
    marginRight: 6
  },
  liveText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#E05345'
  },
  artworkContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 30
  },
  artCircle: {
    width: 170,
    height: 170,
    borderRadius: 85,
    backgroundColor: '#E7EFEA',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
    borderWidth: 2,
    borderColor: '#D4E2D9'
  },
  artIcon: {
    fontSize: 48,
    color: '#4A6B5D'
  },
  mixSubtitle: {
    fontSize: 12,
    fontWeight: '600',
    letterSpacing: 2,
    color: '#7F9387',
    marginBottom: 8
  },
  trackTitle: {
    fontSize: 22,
    fontWeight: '700',
    color: '#26332C',
    textAlign: 'center',
    paddingHorizontal: 16
  },
  artistName: {
    fontSize: 15,
    color: '#6E8075',
    marginTop: 6
  },
  controlsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 24,
    marginBottom: 30
  },
  navButton: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#EBF1ED',
    justifyContent: 'center',
    alignItems: 'center'
  },
  navButtonText: {
    fontSize: 18,
    color: '#4A6B5D'
  },
  playButton: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: '#4A6B5D',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 4,
    shadowColor: '#364B42',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 8
  },
  playButtonText: {
    fontSize: 20,
    fontWeight: '800',
    color: '#FAF7F2',
    letterSpacing: 1
  },
  footer: {
    alignItems: 'center',
    marginBottom: 12
  },
  footerText: {
    fontSize: 12,
    color: '#8C9E94'
  }
});
