import { defineStore } from 'pinia'

/**
 * IMP : Contents Store
 * * Meshroom의 Contents Progress에 대한 전반적인 진행 상태를 저장한다.
 * * contents : Meshroom의 Contents List를 가져온다.
 * ! selectedContents : Contents List에서 선택한 Contents를 저장한다.
 * IMP routeMapping : CurrentContentsId에 따라, Mapping해야 하는 Component Name에 대한 Map
 * IMP currentContentsId : currentContents의 ID Value
 * * contentsSequence : 지금 Contents는 선택된 Contents Curation에서 몇번째 인가?
 * * totalContentsCount : Contents Curation에서 선택된 Contents는 몇 개인가?
 * IMP finishGroupCount : 현재 Contents를 마친, Group은 몇 개인가?
 * * totalGroupCount : 현재 Contents를 진행하고 있는 Group은 몇개인가?
 * IMP currentGroupState : 모든 Group들의 상태에 대한 Information Array => ( subSessionId, isFinish )
 */
export const useContentsStore = defineStore('contents', {
  state: () => ({
    contents: [],
    pickedContents: {},
    selectedContents: [],
    totalDuration: 0,

    routeMapping: {
      0: 'Ending',
      1: 'TOF',
      2: null,
      3: null,
      4: 'alphabet',
      5: null,
      6: null,
      7: 'BallGrowContainer',
      8: null
    },
    socketMapping: {
      1: 'tf',
      2: null,
      3: null,
      4: 'ini-quiz',
      5: null,
      6: null,
      7: 'touch',
      8: null
    },

    currentContentsId: null,
    contentsSequence: null,
    totalContentsCount: null,
    finishGroupCount: null,
    totalGroupCount: null,
    currentContentState: null,
    currentGroupState: [] // Array of group states
  }),
  actions: {
    setContents(contents) {
      this.contents = contents
    },
    setPickedContents(pickedContent) {
      this.pickedContents = pickedContent
    },
    setSelectedContents(newOrder) {
      this.selectedContents = newOrder
    },
    removeContent(content) {
      const index = this.selectedContents.findIndex((c) => c._id === content._id)
      if (index !== -1) {
        this.selectedContents.splice(index, 1) // Remove the content from the array
        this.totalDuration -= content.duration // Decrease the total duration
      }
    },

    addContent(content) {
      const index = this.selectedContents.findIndex((c) => c._id === content._id)
      if (index === -1) {
        this.selectedContents.push(content)
        this.totalDuration += content.duration
      }
    },

    setContentsFinish(isFinish) {
      this.currentContentState = isFinish
    },

    setCurrentContentsState(contentState) {
      this.currentContentsId = contentState.contentsId
      this.contentsSequence = contentState.contentsSequence
      this.totalContentsCount = contentState.totalContentsCount
      this.finishGroupCount = contentState.finishGroupCount
      this.totalGroupCount = contentState.totalGroupCount
      this.currentGroupState = [...contentState.currentGroupState]
    }
  },
  getters: {
    getContents: (state) => state.contents,
    getGroupedContents(state) {
      console.log(state.contents)
      const grouped = {}
      state.contents.forEach((content) => {
        if (!grouped[content.category]) {
          grouped[content.category] = []
        }
        grouped[content.category].push(content)
      })

      return Object.keys(grouped).map((category) => ({
        category,
        items: grouped[category]
      }))
    },
    getPickedContents: (state) => state.pickedContents,
    getSelectedContents: (state) => state.selectedContents,
    getTotalDuration: (state) => state.totalDuration,
    getCurrentContentsId: (state) => state.currentContentsId,
    getContentsSequence: (state) => state.contentsSequence,
    getContentsFinish: (state) => state.currentContentState,
    getRouteMapping: (state) => state.routeMapping,
    getSocketMapping: (state) => state.socketMapping
  },
  persist: {
    key: 'contents-store',
    storage: sessionStorage, // 세션 스토리지에 저장
    paths: ['contents', 'pickedContents'] // 저장할 상태의 경로
  }
})
