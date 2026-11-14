<!-- Two-column frame for login and register: welcome panel on the left, the page's form on the right -->
<template>
  <div class="auth-layout">
    <aside class="auth-intro">
      <p class="auth-brand">Payment Methods</p>
      <h2>Your card or bank account, in one place</h2>
      <p>
        Add a payment method in a minute. Later changes are reviewed by our team, and you can follow
        every step here.
      </p>
      <ul class="auth-points">
        <li>Card and bank details checked as you type</li>
        <li>Every change reviewed by our staff</li>
        <li>Live status of your requests</li>
      </ul>
    </aside>

    <section class="auth-form">
      <slot />
    </section>
  </div>
</template>
