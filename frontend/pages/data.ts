export type RunCompletedResp = {
    uuid: string
    stdout: string
    code: number
    timestamp: Date
    runRequest: {
        language: string
        code: string
    }
    outputs: string[]
}
