import { EventHandler, FormEvent, useEffect, useState } from 'react'
import styles from '../styles/Notebook.module.css'
import hljs from 'highlight.js/lib/common'
import EnvironmentInfo from './Notebook/EnvironmentInfo'

type RunCompletedResp = {
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

const Notebook = () => {
    const [previousRuns, setPreviousRuns] = useState<RunCompletedResp[]>([])
    const [isRunning, setRunning] = useState<string | undefined>(undefined)

    const updateResults = async () => {
        const data: RunCompletedResp[] = await fetch('api/completed').then(
            (res) => res.json()
        )
        setPreviousRuns(data)
        hljs.highlightAll()
        return data
    }

    const runScript = async (event: React.SyntheticEvent) => {
        event.preventDefault()

        const target = event.target as typeof event.target & {
            script: { value: string }
            language: { value: string }
        }

        const res = await fetch('api/submit', {
            body: JSON.stringify({
                language: target.language.value,
                code: target.script.value,
            }),
            headers: {
                'Content-Type': 'application/json',
            },
            method: 'POST',
        })

        const runningUuid = ((await res.json()) as { uuid: string }).uuid

        setRunning(runningUuid)
        const updatesTimer = setInterval(async () => {
            const data = await updateResults()
            if (data.find((item) => item.uuid === runningUuid)) {
                setRunning(undefined)
                clearInterval(updatesTimer)
            }
        }, 1000)
    }

    useEffect(() => {
        updateResults().catch(console.error)
    }, [])

    return (
        <section>
            <div>
                {previousRuns.map((completedRunResp, i) => {
                    const isLastRun: boolean = i + 1 < previousRuns.length
                    return (
                        <div className="card my-1" key={completedRunResp.uuid}>
                            <div className="card-header">
                                {isLastRun && (
                                    <button
                                        className="btn btn-sm"
                                        type="button"
                                        data-bs-toggle="collapse"
                                        data-bs-target={
                                            '#runDetails-' +
                                            completedRunResp.uuid
                                        }
                                        aria-expanded="false"
                                        aria-controls={
                                            'runDetails-' +
                                            completedRunResp.uuid
                                        }
                                    >
                                        <i className="bi bi-arrows-expand"></i>
                                    </button>
                                )}

                                <span className="text-muted mx-2 my-1 card-title">
                                    <span className="badge bg-light text-dark">
                                        {completedRunResp.timestamp}
                                    </span>
                                    <span
                                        className={
                                            'badge float-end' +
                                            (completedRunResp.code == 0
                                                ? ' bg-success'
                                                : ' bg-warning')
                                        }
                                    >
                                        Exit code: {completedRunResp.code}
                                    </span>
                                    <span className="badge bg-light text-dark float-end">
                                        Language:{' '}
                                        {completedRunResp.runRequest.language}
                                    </span>
                                </span>
                            </div>

                            <div
                                id={'runDetails-' + completedRunResp.uuid}
                                className={
                                    'card-body ' + (isLastRun ? 'collapse' : '')
                                }
                            >
                                <pre
                                    className={
                                        styles.sourceCode +
                                        ' bg-light bg-gradient px-2 py-1'
                                    }
                                >
                                    <code
                                        className={
                                            'language-' +
                                            completedRunResp.runRequest.language
                                        }
                                    >
                                        {completedRunResp.runRequest.code}
                                    </code>
                                </pre>
                                <pre key={completedRunResp.uuid}>
                                    {completedRunResp.stdout}
                                </pre>

                                {completedRunResp && (
                                    <ul className="list-group list-group-horizontal">
                                        {completedRunResp.outputs.map((o) => (
                                            <li
                                                key={completedRunResp.uuid}
                                                className="list-group-item fs-6"
                                            >
                                                <i className="bi bi-file-arrow-down"></i>
                                                &nbsp;&nbsp;
                                                <a
                                                    target="_blank"
                                                    rel="noreferrer"
                                                    href={
                                                        '/api/outputs/' +
                                                        completedRunResp.uuid +
                                                        '/' +
                                                        o
                                                    }
                                                >
                                                    {o}
                                                </a>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        </div>
                    )
                })}
            </div>
            {isRunning && (
                <div className="d-flex justify-content-center py-5">
                    <div className="spinner-grow" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                </div>
            )}

            <div>
                <EnvironmentInfo />
                <h3 className="text-center">
                    Get started by entering a script.
                </h3>

                <form onSubmit={runScript}>
                    <div className="form-floating">
                        <select
                            name="language"
                            id="language"
                            className="form-select mb-3"
                            placeholder="Choose programming"
                            defaultValue="r"
                        >
                            <option value="r">R</option>
                            <option value="r-markdown">R Markdown</option>
                            <option value="python">Python</option>
                        </select>
                        <label htmlFor="language">Programming language</label>
                    </div>
                    <div className="form-floating">
                        <textarea
                            className={styles.sourceCodeInput + ' form-control'}
                            name="script"
                            placeholder="Enter script here"
                            id="script"
                            style={{ height: '200px', width: '100%' }}
                            data-gramm="false"
                            data-gramm_editor="false"
                            data-enable-grammarly="false"
                        ></textarea>
                        <label htmlFor="script">Script source code</label>
                    </div>

                    <button type="submit" className="btn btn-primary my-3">
                        Run script
                    </button>
                </form>
            </div>
        </section>
    )
}

export default Notebook
