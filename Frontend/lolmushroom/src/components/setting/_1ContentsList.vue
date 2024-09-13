<script setup>
import { computed, ref } from 'vue'
import { useContentsStore } from '@/stores/contentsStore'
import HeaderComponent from './_1HeaderComponent.vue'
import ListItem from './_1ListItem.vue'

const contentsStore = useContentsStore()
const groupedContents = computed(() => contentsStore.getGroupedContents)

// console.log(category.items)

const pickContents = (item) => {
  if (item.isActive) {
    contentsStore.setPickedContents(item)
  }
}

const addContents = (item) => {
  if (item.isActive) {
    contentsStore.addContent(item)
  }
}

const isItemSelected = (item) => {
  return contentsStore.selectedContents.includes(item)
}
</script>

<template>
  <div class="contents-list">
    <div class="header">
      <HeaderComponent />
    </div>
    <div v-for="(category, index) in groupedContents" :key="index" class="category-section">
      <div class="category-title">{{ category.category }}</div>
      <div class="category-items">
        <ListItem
          v-for="item in category.items"
          :key="item._id"
          :item="item"
          :isItemSelected="isItemSelected(item)"
          @pick="pickContents"
          @add="addContents"
        >
        </ListItem>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contents-list {
  width: 100%;
  max-width: 700px;
  margin: 0 auto;
  padding-left: 20px;
  padding-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: start; /* Align items to the start (left) */
}

.category-section {
  width: 97%;
  margin-top: 20px;
  display: flex;
  flex-direction: column;
}

.category-title {
  font-size: 1.2em;
  font-weight: bold;
  margin-bottom: 10px;
}

/* css about Hover things */
</style>
