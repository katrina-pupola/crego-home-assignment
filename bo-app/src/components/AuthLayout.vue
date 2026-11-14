<!-- Two-column frame for the login page: welcome panel on the left, the form on the right -->
<template>
  <div class="auth-layout">
    <aside class="auth-intro">
      <p class="auth-brand">Payment Methods · Back Office</p>
      <h2>Review client payment method changes</h2>
      <p>
        Every change a client requests waits here for a decision. Approve it to make the new method
        active, or reject it with a reason the client will see.
      </p>
      <ul class="auth-points">
        <li>Queue of pending requests, newest first</li>
        <li>Current and proposed method side by side</li>
        <li>Every decision recorded with reviewer and time</li>
      </ul>
    </aside>

    <section class="auth-form">
      <slot />
    </section>
  </div>
</template>
