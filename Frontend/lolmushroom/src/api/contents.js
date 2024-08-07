import axios from 'axios'

const API_URL = '/api/v1'

export default {
  getContents(success, failure) {
    return axios.get(`${API_URL}/contents`).then(success).catch(failure)
  },
  callNextContents(isStart, success, failure) {
    return axios.get(`${API_URL}/contents/next/${isStart}`).then(success).catch(failure)
  },
  recallContents(success, failure) {
    return axios.get(`${API_URL}/contents/reload`).then(success).catch(failure)
  },
  finishContents(success, failure) {
    return axios.post(`${API_URL}/contents/finish`).then(success).catch(failure)
  },
  // 진실 혹은 거짓 관련 API
  createStatements(sessionId, statesObject, success, failure) {
    return axios.post(`${API_URL}/game/tf/${sessionId}`, statesObject).then(success).catch(failure)
  },
  getStatements(sessionId, success, failure) {
    return axios.get(`${API_URL}/game/tf/${sessionId}`).then(success).catch(failure)
  }
}
