<!-- eslint-disable no-unused-vars -->
<script setup>
import './assets/fonts.css'
import { onMounted } from 'vue'
import { useContentsStore } from './stores/contentsStore'
import contentsAPI from '@/api/contents'
/**
 * * 1. Meshroom의 Contents 목록을 가져와 Pinia에 저장한다.
 * IMP : ContentsAPI.getContents()를 호출하는 fetchContents를 통해 Pinia에 저장
 */

const contentsStore = useContentsStore()
const fetchContents = async () => {
  try {
    const response = await contentsAPI.getContents()
    if (response.data.isSuccess) {
      console.log(response.data)
      contentsStore.setContents(response.data.result.contents)
      contentsStore.setPickedContents(response.data.result.contents['0'])
    }
  } catch (error) {
    console.log('Error Getting Meshroom Contents', error)
  }
}

/**
 * * 2. 모든 사용자는 Meshroom이 OnMount될 때, Contents List를 내려받는다.
 */
onMounted(() => {
  fetchContents()
})
</script>

<template>
  <v-app id="v-app">  
     <RouterView /> 
  </v-app>
</template>

<style scoped>

/* Font 설정*/ 
@font-face {
  font-family: LineFont;
  src:
    url('@/assets/fonts/LINESeedKR-Bd.woff') format('woff'),
    url('@/assets/fonts/LINESeedKR-Bd.woff2') format('woff2');
}

*{
  font-family: LineFont;
}

</style>