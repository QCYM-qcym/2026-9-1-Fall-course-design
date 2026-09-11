import * as api from '../api/auth.js'
import { configureAuthTransport } from '../api/http.js'
import { createAuthState } from '../utils/authState.js'

export const auth = createAuthState(api)
configureAuthTransport({ epoch: () => auth.state.epoch, expire: auth.expire })
