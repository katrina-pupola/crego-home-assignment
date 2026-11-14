<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'
import ResendVerification from '@/components/ResendVerification.vue'

const route = useRoute()
const email = computed(() => (typeof route.query.email === 'string' ? route.query.email : ''))
</script>

<template>
  <AuthLayout>
    <span class="empty-icon" aria-hidden="true">✉️</span>
    <h1>Check your inbox</h1>
    <p>
      We sent a confirmation link to <strong v-if="email">{{ email }}</strong
      ><template v-else>your email address</template>. Click it to finish creating your account.
    </p>
    <p class="muted">The link is valid for 24 hours and can be used once.</p>

    <ResendVerification :email="email || undefined" start-with-cooldown />

    <p class="muted center">Already confirmed? <RouterLink to="/login">Log in</RouterLink></p>
  </AuthLayout>
</template>
