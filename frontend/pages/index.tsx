import type { NextPage } from 'next'
import Head from 'next/head'
import Notebook from './Notebook'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import 'highlight.js/styles/default.css'
import { useEffect } from 'react'

const Home: NextPage = () => {
    useEffect(() => {
        // See: https://stackoverflow.com/questions/67845378/how-can-i-use-bootstrap-5-with-next-js
        typeof document !== undefined
            ? require('bootstrap/dist/js/bootstrap.bundle')
            : null
    }, [])

    return (
        <div>
            <Head>
                <title>Blackbox demo</title>
                <meta
                    name="description"
                    content="This is the demo of Blackbox"
                />
                <meta
                    name="viewport"
                    content="width=device-width, initial-scale=1"
                />
                <link
                    rel="icon"
                    href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🔲</text></svg>"
                />
            </Head>

            <main className="container">
                <h1 className="display-6 my-5">🔲 Blackbox</h1>

                {/* <h3 className={styles.title}>
                    Welcome to{' '}
                    <a href="https://github.com/mkotsur/blackbox">Blackbox</a>{' '}
                    demo!
                </h3> */}

                <Notebook />
            </main>
        </div>
    )
}

export default Home
