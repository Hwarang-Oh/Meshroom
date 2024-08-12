import { defineStore } from 'pinia'

/**
 * IMP : UserStore 정의
 * * userId, userNickName, TeamLeader 여부, Contents Start 여부, userOvToken을 저장하고 있음
 * * 이 정보는 Session Storage에 저장되고 있음.
 * TODO : 왜 userOvToken을 ref로 정의했는가?
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    userId: null,
    userNickname: '닉네임을 설정해주세요',
    isTeamLeader: false,
    isStarted: false,
    userOvToken: null
  }),
  actions: {
    setUserId(userId) {
      this.userId = userId;
    },
    setNickname(nickname) {
      this.userNickname = nickname;
    },
    setTeamLeader(isTeamLeader) {
      this.isTeamLeader = isTeamLeader;
    },
    setIsStarted() {
      this.isStarted = true;
    },
    setOvToken(userOvToken) {
      this.userOvToken = userOvToken;
    }
  },
  getters: {
    getUserId: (state) => state.userId,
    getNickname: (state) => state.userNickname,
    getIsTeamLeader: (state) => state.isTeamLeader,
    getIsStarted: (state) => state.isStarted,
    getUserOvToken: (state) => state.userOvToken,
  },
  persist: {
    key: 'user-store',
    storage: sessionStorage,
    paths: ['userId', 'userNickname', 'isTeamLeader', 'isStarted', 'userOvToken']
  }
});
