/**
 * DEPRECATED
 *
 * This file previously provided a minimal smoke test, but it duplicates
 * `health-stability-test.js` and is kept only for backwards compatibility.
 *
 * Use:
 *   k6 run health-stability-test.js
 */

import http from "k6/http";
import {BASE_URL} from "./auth-cookie-utils.js";

export const options = {vus: 1, duration: "1s"};

export default function () {
    http.get(`${BASE_URL}/actuator/health`, {tags: {endpoint: "health"}});
}