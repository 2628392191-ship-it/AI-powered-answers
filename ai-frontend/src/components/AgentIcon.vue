<template>
  <svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg" class="agent-icon">
    <!-- Body -->
    <circle cx="40" cy="40" r="36" fill="#1a1a2e"/>
    <!-- Screen face -->
    <rect x="18" y="22" width="44" height="30" rx="5" fill="#0f3460"/>
    <rect x="21" y="25" width="38" height="24" rx="3" fill="#16213e"/>
    <!-- Eyes (glowing) -->
    <circle cx="31" cy="35" r="4" fill="#00d4ff" opacity="0.9"/>
    <circle cx="49" cy="35" r="4" fill="#00d4ff" opacity="0.9"/>
    <circle cx="31" cy="35" r="2" fill="#ffffff"/>
    <circle cx="49" cy="35" r="2" fill="#ffffff"/>
    <!-- Mouth / code line -->
    <rect x="27" y="43" width="6" height="2" rx="1" fill="#00d4ff" opacity="0.7"/>
    <rect x="35" y="43" width="10" height="2" rx="1" fill="#e94560" opacity="0.7"/>
    <rect x="47" y="43" width="4" height="2" rx="1" fill="#00d4ff" opacity="0.7"/>
    <!-- Antenna -->
    <line x1="40" y1="22" x2="40" y2="12" stroke="#00d4ff" stroke-width="2"/>
    <circle cx="40" cy="10" r="3" fill="#00d4ff"/>
    <!-- Ears -->
    <rect x="10" y="32" width="8" height="12" rx="3" fill="#0f3460"/>
    <rect x="62" y="32" width="8" height="12" rx="3" fill="#0f3460"/>
    <!-- Legs -->
    <rect x="27" y="56" width="10" height="14" rx="3" fill="#0f3460"/>
    <rect x="43" y="56" width="10" height="14" rx="3" fill="#0f3460"/>
    <!-- Glow effect -->
    <circle cx="40" cy="40" r="36" fill="none" stroke="#00d4ff" stroke-width="1" opacity="0.3"/>
  </svg>
</template>

<style scoped>
.agent-icon {
  width: 100%;
  height: 100%;
}
</style>
