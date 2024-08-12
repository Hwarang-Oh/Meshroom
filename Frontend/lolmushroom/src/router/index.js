import { createRouter, createWebHistory } from 'vue-router'
import MakeSessionView from '@/views/MakeSessionView.vue'
import RoomWatching from '@/components/room/RoomWatching.vue'
import MultiRoom from '@/components/room/MultiRoom.vue'
import ManagerWaiting from '@/components/room/ManagerWaiting.vue'
import PlayerSetting from '@/components/room/playerWaiting/PlayerSetting.vue'
import RoomWaiting from '@/components/room/RoomWaiting.vue'
import ManagerView from '@/views/ManagerView.vue'
import PlayerView from '@/views/PlayerView.vue'
import GroupSessionView from '@/views/GroupSessionView.vue'
import GroupFightSessionView from '@/views/GroupFightSessionView.vue'
import TOFMainComponent from '@/components/contents/tof/TOFMainComponent.vue'
import TOFInputComponent from '@/components/contents/tof/TOFInputComponent.vue'
import AlphabetSubmitComponent from '@/components/contents/alphabet/AlphabetSubmitComponent.vue'
import StartPage from '@/components/setting/_0StartPage.vue'
import CurationPage from '@/components/setting/_1CurationPage.vue'
import SessionCode from '@/components/setting/_2SessionCode.vue'
import BallGrowContainer from '@/components/contents/BallGrow/BallGrowContainer.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: MakeSessionView,
    children: [
      { path: '', name: 'startpage', component: StartPage },
      { path: '/curation', name: 'curation', component: CurationPage },
      { path: '/sessioncode', name: 'sessioncode', component: SessionCode }
    ]
  },

  {
    path: '/admin/:sessionId',
    component: ManagerView,
    children: [
      { path: 'multiroom', name: 'multiroom', component: MultiRoom },
      { path: 'roomwatching', name: 'roomwatching', component: RoomWatching },
      { path: 'managerwaiting', name: 'managerwaiting', component: ManagerWaiting }
    ]
  },

  {
    path: '/:sessionId',
    component: PlayerView,
    children: [
      { path: '', name: 'mainSession', component: PlayerSetting },
      { path: ':subSessionId/roomwaiting', name: 'roomwaiting', component: RoomWaiting },
      {
        path: ':subSessionId/GroupSessionView',
        component: GroupSessionView,
        children: [
          { path: 'TOF', name: 'TOF', component: TOFInputComponent },
          { path: 'TOFContent', name: 'TOFContent', component: TOFMainComponent },
          {
            path: 'alphabet',
            name: 'AlphabetSubmitComponent',
            component: AlphabetSubmitComponent
          }
        ]
      },
      {
        path: ':subSessionId/GroupFightSessionView',
        name: 'GroupFightSessionView',
        component: GroupFightSessionView,
        children: [
          {
            path: '',
            name: 'MushroomGrowContainer',
            component: BallGrowContainer
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory('/'),
  routes
})

export default router
