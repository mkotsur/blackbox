import { useEffect, useState } from 'react'

const formatBytes = (bytes: number, decimals = 2) => {
    if (bytes === 0) return '0 Bytes'

    const k = 1024
    const dm = decimals < 0 ? 0 : decimals
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']

    const i = Math.floor(Math.log(bytes) / Math.log(k))

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i]
}

const EnvironmentInfo = () => {
    type DatasetInfo = [string, number | null]
    type EnvInfo = {
        datasets: DatasetInfo[]
        mountFolders: {
            code: string
            data: string
            output: string
        }
    }

    const [dsInfos, setDsInfos] = useState<EnvInfo | undefined>(undefined)

    const updateAvailableDatasets = async () => {
        const data: EnvInfo = await fetch('api/environment').then((res) =>
            res.json()
        )
        setDsInfos(data)
    }

    useEffect(() => {
        updateAvailableDatasets().catch(console.error)
    })

    return (
        <div className="container py-5">
            <div className="row">
                <div className="col-6">
                    <h5>Datasets in the container:</h5>
                    {dsInfos === undefined ? (
                        'Loading...'
                    ) : (
                        <table>
                            {dsInfos.datasets.map(([path, size]) => {
                                return (
                                    <tr key={path}>
                                        <td className="text-left px-3">
                                            {size === null ? '📂' : '📃'}
                                        </td>
                                        <td className="text-left">
                                            <code className="text-left">
                                                {dsInfos.mountFolders.data +
                                                    '/' +
                                                    path}
                                            </code>
                                        </td>
                                        <td>
                                            <small className="text-muted px-5">
                                                {size === null
                                                    ? ''
                                                    : formatBytes(size)}
                                            </small>
                                        </td>
                                    </tr>
                                )
                            })}
                        </table>
                    )}
                </div>
                <div className="col-sm"></div>
                <div className="col-sm">
                    <h5>Other special folders:</h5>
                    {dsInfos === undefined ? (
                        'Loading...'
                    ) : (
                        <div>
                            <p>
                                You can write files into{' '}
                                <code>{dsInfos.mountFolders.output}</code> and
                                download them.
                            </p>
                            <p>
                                Your script will be run in{' '}
                                <code>{dsInfos.mountFolders.code}</code>
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div>
        // <>
        //     <div>
        //
        //     </div>
        //     <div>
        //         You can download files placed into: <pre>/tmp/out</pre>
        //     </div>
        // </>
    )
}

export default EnvironmentInfo
