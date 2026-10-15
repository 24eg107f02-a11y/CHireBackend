import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return <section className="message-page"><span className="message-code">404</span><h1>Looks like a wrong turn.</h1><p>We couldn't find that page. The opportunity may have moved, or the address may be mistyped.</p><Link className="button button-primary" to="/">Back to CampusHire</Link></section>;
}
