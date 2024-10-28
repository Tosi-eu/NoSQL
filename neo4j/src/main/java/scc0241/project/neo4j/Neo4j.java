package scc0241.project.neo4j;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

import java.util.Scanner;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.Neo4jException;

public class Neo4j {
    private final Driver driver;

    public Neo4j(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    public void close() {
        driver.close();
    }

public void addUser(String username) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {
        String checkUserQuery = "MATCH (u:User {name: $name}) RETURN COUNT(u) > 0 AS exists";
        boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("name", username))
                                    .single()
                                    .get("exists")
                                    .asBoolean();

        if (!userExists) {
            String query = "CREATE (u:User {name: $name, amigos: 0, comments: 0, posts: 0}) RETURN u";
            session.run(query, org.neo4j.driver.Values.parameters("name", username));
            System.out.println("Usuario adicionado: " + username);
        } else {
            System.out.println("Erro: O usuario " + username + " ja existe.");
        }
    } catch (Neo4jException e) {
        if (e.code().equals("Neo.ClientError.Schema.ConstraintValidationFailed")) {
            System.out.println("Erro: O usuario " + username + " ja existe (capturado pela exceção).");
        } else {
            e.printStackTrace();
        }
    }
}

    public void addFriendship(String user1, String user2) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String checkUsersQuery = "MATCH (u1:User {name: $user1}), (u2:User {name: $user2}) RETURN COUNT(u1) > 0 AND COUNT(u2) > 0 AS exists";
            boolean usersExist = session.run(checkUsersQuery, org.neo4j.driver.Values.parameters("user1", user1, "user2", user2))
                                         .single()
                                         .get("exists") 
                                         .asBoolean();

            if (usersExist) {
                String query = "MATCH (u1:User {name: $user1}), (u2:User {name: $user2}) " +
                               "CREATE (u1)-[:FRIEND]->(u2) ";
                session.run(query, org.neo4j.driver.Values.parameters("user1", user1, "user2", user2));
                System.out.println("Amizade adicionada entre " + user1 + " e " + user2);
            } else {
                System.out.println("Erro: Um ou ambos os usuarios nao existem.");
            }
        }
    }

    public void deleteFriendship(String user1, String user2) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String query = "MATCH (u1:User {name: $user1})-[r:FRIEND]->(u2:User {name: $user2}) " +
                           "DELETE r ";
            session.run(query, org.neo4j.driver.Values.parameters("user1", user1, "user2", user2));
            System.out.println("Amizade removida entre " + user1 + " e " + user2);
        }
    }

    public void addPost(String username, String content) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String checkUserQuery = "MATCH (u:User {name: $username}) RETURN COUNT(u) > 0 AS exists";
            boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username))
                                        .single()
                                        .get("exists")
                                        .asBoolean();

            if (userExists) {
                String query = "MATCH (u:User {name: $username}) " +
                               "CREATE (u)-[:POSTED]->(p:Post {content: $content}) ";
                session.run(query, org.neo4j.driver.Values.parameters("username", username, "content", content));
                System.out.println("Post adicionado por " + username + ": " + content);
            } else {
                System.out.println("Erro: O usuario nao existe.");
            }
        }
    }

public void removePost(String username, String content) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {

        String checkUserQuery = "MATCH (u:User {name: $username}) RETURN COUNT(u) > 0 AS userExists";
        boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username))
                                    .single()
                                    .get("userExists")
                                    .asBoolean();

        String checkPostQuery = "MATCH (u:User {name: $username})-[r:POSTED]->(p:Post {content: $content}) RETURN COUNT(p) > 0 AS postExists";
        boolean postExists = session.run(checkPostQuery, org.neo4j.driver.Values.parameters("username", username, "content", content))
                                    .single()
                                    .get("postExists")
                                    .asBoolean();

        if (userExists && postExists) {
            try (var tx = session.beginTransaction()) {
                
                String deleteCommentsQuery = 
                    "MATCH (u:User {name: $username})-[:POSTED]->(p:Post {content: $content})<-[:ON_POST]-(c:Comment) " +
                    "DETACH DELETE c";
                tx.run(deleteCommentsQuery, org.neo4j.driver.Values.parameters("username", username, "content", content));

                String deletePostQuery = 
                    "MATCH (u:User {name: $username})-[r:POSTED]->(p:Post {content: $content}) " +
                    "DETACH DELETE p " ;
                tx.run(deletePostQuery, org.neo4j.driver.Values.parameters("username", username, "content", content));

                tx.commit();
                System.out.println("Post e seus comentarios removidos de " + username + ": " + content);
            } catch (Exception e) {
                throw e;
            }
        } else {
            if (!userExists) {
                System.out.println("Erro: O usuario " + username + " nao existe.");
            }
            if (!postExists) {
                System.out.println("Erro: O post com conteúdo \"" + content + "\" nao existe.");
            }
        }
    }
}

  public void editPost(String username, String oldContent, String newContent) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {
        
        String checkUserQuery = "MATCH (u:User {name: $username}) RETURN COUNT(u) > 0 AS exists";
        boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username))
                                    .single()
                                    .get("exists")
                                    .asBoolean();
        
        if (!userExists) {
            System.out.println("Erro: O usuario " + username + " nao existe.");
            return;
        }

        String checkContentQuery = "MATCH (u:User {name: $username})-[r:POSTED]->(p:Post {content: $oldContent}) RETURN COUNT(p) > 0 AS existsContent";
        boolean contentExists = session.run(checkContentQuery, org.neo4j.driver.Values.parameters("username", username, "oldContent", oldContent))
                                    .single()
                                    .get("existsContent")
                                    .asBoolean();

        if (!contentExists) {
            System.out.println("Erro: O post " + oldContent + " nao existe.");
            return;
        }
        
        String updateQuery = "MATCH (u:User {name: $username})-[r:POSTED]->(p:Post {content: $oldContent}) " +
                             "SET p.content = $newContent";
        session.run(updateQuery, org.neo4j.driver.Values.parameters("username", username, "oldContent", oldContent, "newContent", newContent));
        System.out.println("Post editado de " + username + ": " + oldContent + " -> " + newContent);
    }
}

    
public void editComment(String username, String oldContent, String newContent) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {

        String checkUserQuery = "MATCH (u:User {name: $username}) RETURN COUNT(u) > 0 AS exists";
        boolean userExists = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username))
                                    .single()
                                    .get("exists")
                                    .asBoolean();

        if (!userExists) {
            System.out.println("Erro: O usuario " + username + " nao existe.");
            return;
        }

        String checkCommentQuery = "MATCH (u:User {name: $username})-[r:COMMENTED]->(c:Comment {content: $oldContent}) RETURN COUNT(c) > 0 AS exists";
        boolean commentExists = session.run(checkCommentQuery, org.neo4j.driver.Values.parameters("username", username, "oldContent", oldContent))
                                       .single()
                                       .get("exists")
                                       .asBoolean();

        if (!commentExists) {
            System.out.println("Erro: O comentario \"" + oldContent + "\" nao existe.");
            return;
        }

        String query = "MATCH (u:User {name: $username})-[r:COMMENTED]->(c:Comment {content: $oldContent}) " +
                       "SET c.content = $newContent";
        session.run(query, org.neo4j.driver.Values.parameters("username", username, "oldContent", oldContent, "newContent", newContent));
        System.out.println("Comentario editado de " + username + ": " + oldContent + " -> " + newContent);
    } catch (Exception e) {
        System.out.println("Erro ao editar o comentario: " + e.getMessage());
    }
}

    public void adicionarComentario(String postOwner, String commenter, String postContent, String commentContent) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork")))  {
            String checkUsersQuery = "MATCH (postOwner:User {name: $postOwner}), (commenter:User {name: $commenter}) " +
                                     "RETURN COUNT(postOwner) > 0 AND COUNT(commenter) > 0 AS usersExist";
            boolean usersExist = session.run(checkUsersQuery, org.neo4j.driver.Values.parameters("postOwner", postOwner, "commenter", commenter))
                                         .single()
                                         .get("usersExist")
                                         .asBoolean();

            if (usersExist) {
                String checkPostQuery = "MATCH (postOwner:User {name: $postOwner})-[:POSTED]->(p:Post {content: $postContent}) RETURN COUNT(p) > 0 AS postExists";
                boolean postExists = session.run(checkPostQuery, org.neo4j.driver.Values.parameters("postOwner", postOwner, "postContent", postContent))
                                             .single()
                                             .get("postExists")
                                             .asBoolean();

                if (postExists) {
                    String addCommentQuery = "MATCH (commenter:User {name: $commenter}), (p:Post {content: $postContent}) " +
                                             "CREATE (commenter)-[:COMMENTED]->(c:Comment {content: $commentContent}), " +
                                             "(c)-[:ON_POST]->(p)" +
                                             "WITH commenter " +
                                             "MATCH (commenter) SET commenter.comments = commenter.comments + 1";
                    session.run(addCommentQuery, org.neo4j.driver.Values.parameters("commenter", commenter, "postContent", postContent, "commentContent", commentContent));
                    System.out.println("Comentario adicionado por " + commenter + " no post de " + postOwner);
                } else {
                    System.out.println("Erro: O post com o conteúdo \"" + postContent + "\" nao existe.");
                }
            } else {
                System.out.println("Erro: Um ou ambos os usuarios nao existem.");
            }
        }
    }

 public void deleteUser(String username) {
    try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase("socialnetwork"))) {
        String checkUserQuery = "MATCH (u:User {name: $username}) RETURN u";
        var userResult = session.run(checkUserQuery, org.neo4j.driver.Values.parameters("username", username));

        if (userResult.hasNext()) {
            try (var tx = session.beginTransaction()) {
                String deleteUserCommentsQuery = 
                    "MATCH (u:User {name: $username})-[r:COMMENTED]->(c:Comment) " +
                    "DETACH DELETE c ";

                tx.run(deleteUserCommentsQuery, org.neo4j.driver.Values.parameters("username", username));

                String deleteCommentsOnUserPostsQuery = 
                    "MATCH (u:User {name: $username})-[:POSTED]->(p:Post)<-[:ON_POST]-(c:Comment) " +
                    "DETACH DELETE c";

                tx.run(deleteCommentsOnUserPostsQuery, org.neo4j.driver.Values.parameters("username", username));

                String deleteUserPostsQuery = 
                    "MATCH (u:User {name: $username})-[:POSTED]->(p:Post) " +
                    "DETACH DELETE p";

                tx.run(deleteUserPostsQuery, org.neo4j.driver.Values.parameters("username", username));

                String detachUserQuery = 
                    "MATCH (u:User {name: $username}) " +
                    "DETACH DELETE u";

                tx.run(detachUserQuery, org.neo4j.driver.Values.parameters("username", username));
                tx.commit();

                System.out.println("Usuario e todos os relacionamentos e nodes associados foram removidos: " + username);
            }
        } else {
            System.out.println("Erro: O usuario nao existe.");
        }
    }
}
 
   public void openNeo4jBrowser() {
        String url = "http://localhost:7474/browser/";
        
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("Neo4j Browser aberto!");
            } catch (IOException | URISyntaxException e) {
                System.out.println("Erro ao tentar abrir o Neo4j Browser: " + e.getMessage());
            }
        } else {
            System.out.println("Desktop nao suportado. Nao foi possivel abrir o navegador.");
        }
    }

      public void verifyGraph() {
        System.out.println("===== Relatorio de Verificacao do Grafo =====");

        try (Session session = driver.session(SessionConfig.forDatabase("socialnetwork"))) {

            String nodeCountQuery = "MATCH (n) RETURN count(n) AS nodeCount";
            int nodeCount = session.run(nodeCountQuery).single().get("nodeCount").asInt();
            System.out.println("Total de nodes: " + nodeCount);

            String friendshipCountQuery = "MATCH (:User)-[r:FRIENDS_WITH]->(:User) RETURN count(r) AS friendshipCount";
            int friendshipCount = session.run(friendshipCountQuery).single().get("friendshipCount").asInt();
            System.out.println("Total de amizades: " + friendshipCount);

            String postsQuery = "MATCH (u:User)-[:POSTED]->(p:Post) RETURN u.name AS username, p.content AS postContent";
            var postsResult = session.run(postsQuery);
            System.out.println("\nPosts realizados:");
            postsResult.list().forEach(record ->
                System.out.println(" - Usuario: " + record.get("username").asString() +
                                   " | Postagem: " + record.get("postContent").asString()));

         String commentsQuery = "MATCH (u:User)-[:COMMENTED]->(c:Comment)-[:ON_POST]->(p:Post) " +
                                "WHERE u.name IS NOT NULL AND c.content IS NOT NULL AND p.content IS NOT NULL "  +
                                "RETURN u.name AS user, c.content AS comment, p.content AS post " +
                                "ORDER BY user";


            var commentsResult = session.run(commentsQuery);
            System.out.println("\nComentarios realizados:");
            commentsResult.list().forEach(record ->
                System.out.println(" - Usuario: " + record.get("user").asString() +
                                   " | Comentario: " + record.get("comment").asString() +
                                   " | No post: " + record.get("post").asString()));

        } catch (Exception e) {
            System.out.println("Erro ao gerar o relatorio do grafo: " + e.getMessage());
        }
    }
      
    public static void main(String... args) {
        final String dbUri = "neo4j://localhost:7687";
        final String dbUser = "neo4j";
        final String dbPassword = "neo4j_Eu12";

        try {
            Neo4j socialNetwork = new Neo4j(dbUri, dbUser, dbPassword);
            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.println("\nMenu:");
                System.out.println("1. Criar Usuario");
                System.out.println("2. Adicionar Amizade");
                System.out.println("3. Remover Amizade");
                System.out.println("4. Adicionar Post");
                System.out.println("5. Remover Post");
                System.out.println("6. Editar Post");
                System.out.println("7. Adicionar Comentario");
                System.out.println("8. Editar Comentario");
                System.out.println("9. Remover Usuario");
                System.out.println("10. Abrir Neo4j Browser");
                System.out.println("11. Gerar relatorio do Grafo");
                System.out.println("12. Sair");
                System.out.print("Escolha uma opcao: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1 -> {
                        System.out.print("Digite o nome do usuario: ");
                        String username = scanner.nextLine();
                        socialNetwork.addUser(username);
                    }
                    case 2 -> {
                        System.out.print("Digite o nome do primeiro usuario: ");
                        String user1 = scanner.nextLine();
                        System.out.print("Digite o nome do segundo usuario: ");
                        String user2 = scanner.nextLine();
                        socialNetwork.addFriendship(user1, user2);
                    }
                    case 3 -> {
                        System.out.print("Digite o nome do primeiro usuario: ");
                        String user1ToRemove = scanner.nextLine();
                        System.out.print("Digite o nome do segundo usuario: ");
                        String user2ToRemove = scanner.nextLine();
                        socialNetwork.deleteFriendship(user1ToRemove, user2ToRemove);
                    }
                    case 4 -> {
                        System.out.print("Digite o nome do usuario: ");
                        String postUser = scanner.nextLine();
                        System.out.print("Digite o conteúdo do post: ");
                        String postContent = scanner.nextLine();
                        socialNetwork.addPost(postUser, postContent);
                    }
                    case 5 -> {
                        System.out.print("Digite o nome do usuario: ");
                        String removePostUser = scanner.nextLine();
                        System.out.print("Digite o conteúdo do post a ser removido: ");
                        String removePostContent = scanner.nextLine();
                        socialNetwork.removePost(removePostUser, removePostContent);
                    }
                    case 6 -> {
                        System.out.print("Digite o nome do usuario: ");
                        String editPostUser = scanner.nextLine();
                        System.out.print("Digite o conteúdo antigo do post: ");
                        String oldPostContent = scanner.nextLine();
                        System.out.print("Digite o novo conteúdo do post: ");
                        String newPostContent = scanner.nextLine();
                        socialNetwork.editPost(editPostUser, oldPostContent, newPostContent);
                    }
                    case 7 -> {
                        System.out.print("Digite o nome do dono do post: ");
                        String postOwner = scanner.nextLine();
                        System.out.print("Digite o nome do usuario que esta comentando: ");
                        String commenter = scanner.nextLine();
                        System.out.print("Digite o conteúdo do post: ");
                        String postContent = scanner.nextLine();
                        System.out.print("Digite o conteúdo do comentario: ");
                        String commentContent = scanner.nextLine();
                        socialNetwork.adicionarComentario(postOwner, commenter, postContent, commentContent);
                    }
                    case 8 -> {
                        System.out.print("Digite o nome do usuario que comentou: ");
                        String userToEditComment = scanner.nextLine();
                        System.out.print("Comentario anterior: ");
                        String oldCommentContent = scanner.nextLine();
                        System.out.print("Novo comentario: ");
                        String newCommentContent = scanner.nextLine();
                        socialNetwork.editComment(userToEditComment, oldCommentContent, newCommentContent);
                    }
                    case 9 -> {
                        System.out.print("Digite o nome do usuario a ser removido: ");
                        String userToDelete = scanner.nextLine();
                        socialNetwork.deleteUser(userToDelete);
                    }
                    case 10 -> socialNetwork.openNeo4jBrowser();
                    case 11 -> socialNetwork.verifyGraph();
                    case 12 -> running = false;
                    default -> System.out.println("Opcao invalida. Tente novamente.");
                }
            }

            socialNetwork.close();
        } catch (ServiceUnavailableException e) {
            System.out.println("Erro ao conectar-se ao Neo4j: " + e.getMessage());
        }
    }
}